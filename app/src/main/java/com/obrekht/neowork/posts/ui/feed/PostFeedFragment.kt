package com.obrekht.neowork.posts.ui.feed

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.navigateToSignUp
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.databinding.FragmentPostFeedBinding
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteConfirmationDialogFragment
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteElementType
import com.obrekht.neowork.posts.ui.navigateToPost
import com.obrekht.neowork.posts.ui.navigateToPostEditor
import com.obrekht.neowork.posts.ui.sharePost
import com.obrekht.neowork.posts.ui.showDeleteConfirmation
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PUBLISHED_DATE_REFRESH_INTERVAL: Long = 1000 // In milliseconds

@AndroidEntryPoint
class PostFeedFragment : Fragment(R.layout.fragment_post_feed) {

    private val binding by viewBinding(FragmentPostFeedBinding::bind)
    private val viewModel: PostFeedViewModel by viewModels()

    private val uiState: FeedUiState
        get() = viewModel.uiState.value

    private var snackbar: Snackbar? = null
    private var adapter: PostsAdapter? = null

    private val interactionListener = object : PostInteractionListener {
        override fun onClick(post: Post) {
            navigateToPost(post.id)
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
                showDeleteConfirmation(post.id, DeleteElementType.POST)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        with(binding.toolbar) {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.log_in -> {
                        navigateToLogIn()
                        true
                    }

                    R.id.sign_up -> {
                        navigateToSignUp()
                        true
                    }

                    R.id.profile -> {
                        // TODO: Open profile
                        viewModel.logOut()
                        true
                    }

                    else -> false
                }
            }
        }

        with(binding) {
            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            adapter = PostsAdapter(interactionListener).apply {
                postListView.adapter = withLoadStateHeaderAndFooter(
                    header = PostLoadStateAdapter(::retry),
                    footer = PostLoadStateAdapter(::retry)
                )
            }.also { adapter ->
                viewLifecycleScope.launch {
                    viewLifecycleOwner.repeatOnStarted {
                        launch {
                            adapter.loadStateFlow.collectLatest(::handleLoadState)
                        }
                        launch {
                            adapter.loadStateFlow
                                .distinctUntilChangedBy { it.refresh }
                                .filter { it.refresh is LoadState.NotLoading }
                                .collect {
                                    postListView.scrollToPosition(0)
                                }
                        }
//                        launch {
//                            adapter.onPagesUpdatedFlow.collectLatest {
//                                if (!uiState.scrollDone) {
//                                    val postPosition =
//                                        adapter.snapshot().indexOfFirst {
//                                            (it as? PostItem)?.let { item ->
//                                                item.post.id == args.updatedPostId
//                                            } ?: false
//                                        }
//                                    if (postPosition >= 0) {
//                                        postListView.scrollToPosition(postPosition)
//                                        viewModel.scrollDone()
//                                    }
//                                }
//                            }
//                        }
                    }
                }
            }

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
                viewModel.data.onEach { adapter?.submitData(it) }.launchIn(this)
                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)

                startRefreshingPublishedDate()
            }
        }
    }

    private fun refresh() {
        adapter?.refresh()
        binding.buttonNewPosts.hide()
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleState(state: FeedUiState) {
        with(binding) {
            toolbar.menu.setGroupVisible(R.id.unauthenticated, !state.isLoggedIn)
            toolbar.menu.setGroupVisible(R.id.authenticated, state.isLoggedIn)

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
        adapter?.let { adapter ->
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

            if (state.refresh is LoadState.Error) {
                viewModel.updateDataState(DataState.Error)

                if (adapter.itemCount == 0) {
                    errorGroup.isVisible = true
                } else {
                    errorGroup.isVisible = false
                    showErrorSnackbar(R.string.error_loading) {
                        refresh()
                    }
                }
            } else {
                errorGroup.isVisible = false
            }
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            Event.ErrorLoadPosts -> {
                showErrorSnackbar(R.string.error_loading) {
                    refresh()
                }
            }

            is Event.ErrorLikingPost -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLikeById(event.postId)
                }
            }

            is Event.ErrorRemovingPost -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.deleteById(event.postId)
                }
            }
        }
    }

    private suspend fun startRefreshingPublishedDate() {
        while (currentCoroutineContext().isActive) {
            adapter?.let {
                for (position in 0 until it.itemCount) {
                    val holder = (binding.postListView.findViewHolderForAdapterPosition(position) as? PostViewHolder)
                    holder?.refreshPublishedDate()
                }
            }

            delay(PUBLISHED_DATE_REFRESH_INTERVAL)
        }
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.buttonAddPost, resId, Snackbar.LENGTH_INDEFINITE).apply {
            anchorView = binding.buttonAddPost
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}