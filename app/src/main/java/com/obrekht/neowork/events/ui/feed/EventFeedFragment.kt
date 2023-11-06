package com.obrekht.neowork.events.ui.feed

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.core.ui.mainscreen.MainScreenFragment
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.databinding.FragmentEventFeedBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.editor.ui.editor.EditorFragment
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.ui.common.EventInteractionListener
import com.obrekht.neowork.events.ui.common.EventItem
import com.obrekht.neowork.events.ui.common.EventListAdapter
import com.obrekht.neowork.events.ui.common.EventLoadStateAdapter
import com.obrekht.neowork.events.ui.common.EventViewHolder
import com.obrekht.neowork.events.ui.navigateToEvent
import com.obrekht.neowork.events.ui.navigateToEventEditor
import com.obrekht.neowork.events.ui.shareEvent
import com.obrekht.neowork.users.ui.navigateToUserProfile
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException

private const val PUBLISHED_DATE_REFRESH_INTERVAL: Long = 1000 // ms

@AndroidEntryPoint
class EventFeedFragment : Fragment(R.layout.fragment_event_feed) {

    private val binding by viewBinding(FragmentEventFeedBinding::bind)
    private val viewModel: EventFeedViewModel by viewModels()

    private var snackbar: Snackbar? = null
    private var adapter: EventListAdapter? = null

    private val interactionListener = object : EventInteractionListener {
        override fun onClick(event: Event) {
            navigateToEvent(event.id)
        }

        override fun onAvatarClick(event: Event) {
            navigateToUserProfile(event.authorId)
        }

        override fun onLike(event: Event) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleLike(event)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onShare(event: Event) {
            shareEvent(event)
        }

        override fun onParticipate(event: Event) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleParticipation(event)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onEdit(event: Event) {
            if (viewModel.isLoggedIn) {
                navigateToEventEditor(event.id)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onDelete(event: Event) {
            if (viewModel.isLoggedIn) {
                showDeleteConfirmation(event.id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult(
            MainScreenFragment.REQUEST_KEY_SCROLL_TARGET,
            bundleOf(MainScreenFragment.RESULT_TARGET_VIEW_ID to R.id.event_list_view)
        )

        binding.eventListView.setBarsInsetsListener {
            setPadding(
                paddingLeft, paddingRight, paddingTop,
                resources.getDimension(R.dimen.fab_bottom_padding).toInt()
            )
        }

        view.setBarsInsetsListener {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it.bottom
            }
        }

        with(DeleteConfirmationDialogFragment) {
            setFragmentResultListener(
                getRequestKey(DeleteElementType.EVENT)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    val eventId = bundle.getLong(RESULT_ELEMENT_ID)
                    viewModel.deleteById(eventId)
                }
            }
        }

        setFragmentResultListener(
            SuggestAuthDialogFragment.REQUEST_KEY
        ) { _, bundle ->
            val positive = bundle.getBoolean(SuggestAuthDialogFragment.RESULT_POSITIVE)
            if (positive) {
                navigateToLogIn()
            }
        }

        val adapter = EventListAdapter(interactionListener).apply {
            binding.eventListView.adapter = withLoadStateHeaderAndFooter(
                header = EventLoadStateAdapter(::retry),
                footer = EventLoadStateAdapter(::retry)
            )
        }
        this.adapter = adapter

        with(binding) {
            swipeRefresh.setOnRefreshListener { refresh() }
            retryButton.setOnClickListener { refresh() }

            buttonAddEvent.setOnClickListener {
                if (viewModel.isLoggedIn) {
                    navigateToEventEditor()
                } else {
                    showSuggestAuthDialog()
                }
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                launch {
                    adapter.loadStateFlow.collectLatest(::handleLoadState)
                }
                launch {
                    adapter.onPagesUpdatedFlow.collect {
                        tryScrollToEvent()
                    }
                }

                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)

                viewModel.event.onEach(::handleEvent).launchIn(this)

                startRefreshingPublishedDate()
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleLoadState(state: CombinedLoadStates): Unit = with(binding) {
        val adapter = adapter ?: return

        if (state.refresh is LoadState.NotLoading) {
            viewModel.updateDataState(DataState.Success)

            if (state.append.endOfPaginationReached) {
                emptyText.isVisible = adapter.itemCount == 0
            }
        } else {
            emptyText.isVisible = false
        }

        if (state.refresh is LoadState.Loading) {
            viewModel.updateDataState(DataState.Loading)

            snackbar?.dismiss()
            if (adapter.itemCount == 0) {
                progress.show()
                swipeRefresh.isEnabled = false
            } else {
                progress.hide()
                swipeRefresh.isRefreshing = true
            }
        } else if (state.source.refresh !is LoadState.Loading) {
            progress.hide()
            swipeRefresh.isEnabled = true
            swipeRefresh.isRefreshing = false
        }

        (state.refresh as? LoadState.Error)?.let {
            viewModel.updateDataState(DataState.Error)

            when (it.error) {
                is HttpException -> showErrorSnackbar(R.string.error_loading_users) {
                    refresh()
                }

                is ConnectException -> showErrorSnackbar(R.string.error_connection) {
                    refresh()
                }

                else -> showErrorSnackbar(R.string.error_loading) {
                    refresh()
                }
            }
            errorGroup.isVisible = adapter.itemCount == 0
        } ?: {
            errorGroup.isVisible = false
        }

        Unit
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ErrorLikingEvent -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLikeById(event.eventId)
                }
            }

            is UiEvent.ErrorParticipatingEvent -> {
                showErrorSnackbar(R.string.error_participating) {
                    viewModel.toggleParticipationById(event.eventId)
                }
            }

            is UiEvent.ErrorDeleting -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.deleteById(event.eventId)
                }
            }
        }
    }

    private fun refresh() {
        adapter?.refresh()
    }

    private fun tryScrollToEvent() {
        val adapter = adapter ?: return

        findRootNavController().currentBackStackEntry?.run {
            val eventId = savedStateHandle.get<Long>(EditorFragment.KEY_SCROLL_TO_ID) ?: return
            val eventPosition = adapter.snapshot().indexOfFirst {
                (it as? EventItem)?.let { item ->
                    item.event.id == eventId
                } ?: false
            }
            if (eventPosition >= 0) {
                binding.eventListView.scrollToPosition(eventPosition)
                savedStateHandle.remove<Long>(EditorFragment.KEY_SCROLL_TO_ID)
            }
        }
    }

    private suspend fun startRefreshingPublishedDate() {
        while (currentCoroutineContext().isActive) {
            adapter?.let {
                for (position in 0 until it.itemCount) {
                    val holder =
                        (binding.eventListView.findViewHolderForAdapterPosition(position) as? EventViewHolder)
                    holder?.refreshPublishedDate()
                }
            }

            delay(PUBLISHED_DATE_REFRESH_INTERVAL)
        }
    }

    private fun showDeleteConfirmation(eventId: Long) {
        val action = EventFeedFragmentDirections.actionOpenDeleteConfirmation(
            eventId,
            DeleteElementType.POST
        )
        findNavController().navigate(action)
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.buttonAddEvent, resId, Snackbar.LENGTH_LONG).apply {
            anchorView = binding.buttonAddEvent
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}

