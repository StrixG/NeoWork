package com.obrekht.neowork.users.ui.profile.wall

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.UserProfileGraphDirections
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentWallBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.media.ui.navigateToMediaView
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.common.PostInteractionListener
import com.obrekht.neowork.posts.ui.common.PostListAdapter
import com.obrekht.neowork.posts.ui.navigateToPost
import com.obrekht.neowork.posts.ui.navigateToPostEditor
import com.obrekht.neowork.posts.ui.sharePost
import com.obrekht.neowork.users.ui.navigateToUserProfile
import com.obrekht.neowork.users.ui.profile.UserProfileViewModel
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException

@AndroidEntryPoint
class WallFragment : Fragment(R.layout.fragment_wall) {

    private val binding by viewBinding(FragmentWallBinding::bind)
    private val viewModel: WallViewModel by viewModels()
    private val userProfileViewModel: UserProfileViewModel
            by hiltNavGraphViewModels(R.id.user_profile_graph)

    private val userId: Long
        get() = arguments?.getLong(WallArguments.USER_ID) ?: 0L

    private var snackbar: Snackbar? = null
    private var adapter: PostListAdapter? = null

    private val interactionListener = object : PostInteractionListener {
        override fun onClick(post: Post) {
            navigateToPost(post.id)
        }

        override fun onAvatarClick(post: Post) {
            if (post.authorId != userId) {
                navigateToUserProfile(post.authorId)
            }
        }

        override fun onAttachmentClick(post: Post, view: ImageView) {
            post.attachment?.let {
                navigateToMediaView(it.type, it.url, view)
            }
        }

        override fun onPlayAudioButtonClick(post: Post) {
            post.attachment?.let {
                if (it.type == AttachmentType.AUDIO) {
                    viewModel.playAudio(it.url)
                }
            }
        }

        override fun onLike(post: Post): Boolean {
            return if (userProfileViewModel.isLoggedIn) {
                viewModel.togglePostLike(post)
                true
            } else {
                showSuggestAuthDialog()
                false
            }
        }

        override fun onShare(post: Post) {
            sharePost(post)
        }

        override fun onEdit(post: Post) {
            if (userProfileViewModel.isLoggedIn) {
                navigateToPostEditor(post.id)
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onDelete(post: Post) {
            if (userProfileViewModel.isLoggedIn) {
                showDeleteConfirmation(post.id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(DeleteConfirmationDialogFragment) {
            parentFragment?.setFragmentResultListener(
                getRequestKey(DeleteElementType.POST)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    val postId = bundle.getLong(RESULT_ELEMENT_ID)
                    viewModel.deletePostById(postId)
                }
            }
        }

        val adapter = PostListAdapter(interactionListener)
        this.adapter = adapter

        with(binding) {
            postListView.adapter = adapter
            postListView.setBarsInsetsListener {
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    it.bottom
                )
            }

            swipeRefresh.setOnRefreshListener {
                refresh()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                launch {
                    adapter.loadStateFlow.collectLatest(::handleLoadState)
                }

                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleLoadState(state: CombinedLoadStates): Unit = with(binding) {
        adapter?.let { adapter ->
            if (state.refresh is LoadState.NotLoading) {
                viewModel.updateDataState(DataState.Success)

                if (state.append.endOfPaginationReached) {
                    val isVisible = adapter.itemCount == 0
                    emptyText.isVisible = isVisible
                }
            } else {
                emptyText.isVisible = false
            }

            if (state.refresh is LoadState.Loading) {
                viewModel.updateDataState(DataState.Loading)

                snackbar?.dismiss()
                if (adapter.itemCount == 0) {
                    swipeRefresh.isEnabled = false
                } else {
                    swipeRefresh.isRefreshing = true
                }
            } else if (state.source.refresh !is LoadState.Loading) {
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
            }
        }
    }

    private fun refresh() {
        userProfileViewModel.refresh()
        adapter?.refresh()
    }

    private fun showDeleteConfirmation(postId: Long) {
        val action = UserProfileGraphDirections.actionOpenDeleteConfirmation(
            postId,
            DeleteElementType.POST
        )
        findNavController().navigate(action)
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.root, resId, Snackbar.LENGTH_LONG).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }

    companion object {
        fun newInstance(userId: Long): WallFragment {
            return WallFragment().apply {
                arguments = bundleOf(
                    WallArguments.USER_ID to userId
                )
            }
        }
    }
}
