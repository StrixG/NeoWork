package com.obrekht.neowork.posts.ui.feed

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.core.ui.MainFragment
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.databinding.FragmentPostFeedBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.editor.ui.editor.EditorFragment
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.common.PostInteractionListener
import com.obrekht.neowork.posts.ui.common.PostItem
import com.obrekht.neowork.posts.ui.common.PostListAdapter
import com.obrekht.neowork.posts.ui.common.PostLoadStateAdapter
import com.obrekht.neowork.posts.ui.common.PostViewHolder
import com.obrekht.neowork.posts.ui.navigateToPost
import com.obrekht.neowork.posts.ui.navigateToPostEditor
import com.obrekht.neowork.posts.ui.sharePost
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

private const val PUBLISHED_DATE_REFRESH_INTERVAL: Long = 1000 // In milliseconds

@AndroidEntryPoint
class PostFeedFragment : Fragment(R.layout.fragment_post_feed) {

    private val binding by viewBinding(FragmentPostFeedBinding::bind)
    private val viewModel: PostFeedViewModel by viewModels()

    private val uiState: PostFeedUiState
        get() = viewModel.uiState.value

    private var snackbar: Snackbar? = null
    private var adapter: PostListAdapter? = null

    private val interactionListener = object : PostInteractionListener {
        override fun onClick(post: Post) {
            navigateToPost(post.id)
        }

        override fun onAvatarClick(post: Post) {
            navigateToUserProfile(post.authorId)
        }

        override fun onLike(post: Post) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleLike(post)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onShare(post: Post) {
            sharePost(post)
        }

        override fun onEdit(post: Post) {
            if (viewModel.isLoggedIn) {
                navigateToPostEditor(post.id)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onDelete(post: Post) {
            if (viewModel.isLoggedIn) {
                showDeleteConfirmation(post.id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult(
            MainFragment.REQUEST_KEY_SCROLL_TARGET,
            bundleOf(MainFragment.RESULT_TARGET_VIEW_ID to R.id.post_list_view)
        )

        binding.postListView.setBarsInsetsListener {
            setPadding(
                paddingLeft, paddingRight, paddingTop,
                resources.getDimension(R.dimen.fab_bottom_padding).toInt()
            )
        }

        view.setBarsInsetsListener {
            updateLayoutParams<MarginLayoutParams> {
                bottomMargin = it.bottom
            }
        }

        with(DeleteConfirmationDialogFragment) {
            setFragmentResultListener(
                getRequestKey(DeleteElementType.POST)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    val postId = bundle.getLong(RESULT_ELEMENT_ID)
                    viewModel.deleteById(postId)
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

        val adapter = PostListAdapter(interactionListener).apply {
            binding.postListView.adapter = withLoadStateHeaderAndFooter(
                header = PostLoadStateAdapter(::retry),
                footer = PostLoadStateAdapter(::retry)
            )
        }
        this.adapter = adapter

        with(binding) {
            // Hide new posts button on scroll down and show on scroll up
            postListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && buttonNewPosts.isShown) {
                        buttonNewPosts.hide()
                    } else if (dy < 0 && !buttonNewPosts.isShown &&
                        !swipeRefresh.isRefreshing && uiState.newerCount > 0
                    ) {
                        buttonNewPosts.show()
                    }
                }
            })

            swipeRefresh.setOnRefreshListener { refresh() }
            retryButton.setOnClickListener { refresh() }

            buttonAddPost.setOnClickListener {
                if (viewModel.isLoggedIn) {
                    navigateToPostEditor()
                } else {
                    showSuggestAuthDialog()
                }
            }

            buttonNewPosts.setOnClickListener {
                buttonNewPosts.hide()
                viewModel.showNewPosts()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                launch {
                    adapter.loadStateFlow.collectLatest(::handleLoadState)
                }
                launch {
                    adapter.onPagesUpdatedFlow.collect {
                        tryScrollToPost()
                    }
                }

                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)

                viewModel.uiState.onEach(::handleState).launchIn(this)
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

    private fun handleState(state: PostFeedUiState) {
        with(binding) {
            if (state.dataState == DataState.Success) {
                if (state.newerCount > 0) {
                    buttonNewPosts.text = getString(R.string.new_posts, state.newerCount)
                    buttonNewPosts.show()
                } else {
                    buttonNewPosts.hide()
                }
            } else {
                buttonNewPosts.hide()
            }
        }
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
            is UiEvent.ErrorLikingPost -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLikeById(event.postId)
                }
            }

            is UiEvent.ErrorDeleting -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.deleteById(event.postId)
                }
            }
        }
    }

    private fun refresh() {
        adapter?.refresh()
        binding.buttonNewPosts.hide()
    }

    private fun tryScrollToPost() {
        val adapter = adapter ?: return

        findRootNavController().currentBackStackEntry?.run {
            val postId = savedStateHandle.get<Long>(EditorFragment.KEY_SCROLL_TO_ID) ?: return
            val postPosition = adapter.snapshot().indexOfFirst {
                (it as? PostItem)?.let { item ->
                    item.post.id == postId
                } ?: false
            }
            if (postPosition >= 0) {
                binding.postListView.scrollToPosition(postPosition)
                savedStateHandle.remove<Long>(EditorFragment.KEY_SCROLL_TO_ID)
            }
        }
    }

    private suspend fun startRefreshingPublishedDate() {
        while (currentCoroutineContext().isActive) {
            adapter?.let {
                for (position in 0 until it.itemCount) {
                    val holder =
                        (binding.postListView.findViewHolderForAdapterPosition(position) as? PostViewHolder)
                    holder?.refreshPublishedDate()
                }
            }

            delay(PUBLISHED_DATE_REFRESH_INTERVAL)
        }
    }

    private fun showDeleteConfirmation(postId: Long) {
        val action = PostFeedFragmentDirections.actionOpenDeleteConfirmation(
            postId,
            DeleteElementType.POST
        )
        findNavController().navigate(action)
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.buttonAddPost, resId, Snackbar.LENGTH_LONG).apply {
            anchorView = binding.buttonAddPost
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}
