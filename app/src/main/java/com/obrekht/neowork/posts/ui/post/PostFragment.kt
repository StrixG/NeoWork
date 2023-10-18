package com.obrekht.neowork.posts.ui.post

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.navigateToSignUp
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentPostBinding
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteConfirmationDialogFragment
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteElementType
import com.obrekht.neowork.posts.ui.feed.PostInteractionListener
import com.obrekht.neowork.posts.ui.navigateToPostEditor
import com.obrekht.neowork.posts.ui.sharePost
import com.obrekht.neowork.posts.ui.showDeleteConfirmation
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import com.obrekht.neowork.utils.hideKeyboard
import com.obrekht.neowork.utils.makeLinks
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostFragment : Fragment(R.layout.fragment_post) {
    private val binding by viewBinding(FragmentPostBinding::bind)
    private val viewModel: PostViewModel
            by hiltNavGraphViewModels(R.id.post_graph)

    private lateinit var post: Post
    private var snackbar: Snackbar? = null
    private var commentsAdapter: CommentsAdapter? = null

    private val interactionListener = object : PostInteractionListener {
        override fun onLike(post: Post) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleLike()
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
            }
        }

        override fun onDelete(post: Post) {
            if (viewModel.isLoggedIn) {
                showDeleteConfirmation(post.id, DeleteElementType.POST)
            }
        }
    }

    private val commentInteractionListener = object : CommentInteractionListener {
        override fun onClick(comment: Comment) {
            val action =
                PostFragmentDirections.actionOpenCommentOptions(comment.id, comment.ownedByMe)
            findNavController().navigate(action)
        }

        override fun onLike(comment: Comment) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleCommentLike(comment)
            } else {
                showSuggestAuthDialog()
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
                    viewModel.remove()
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

        with(binding) {
            commentInputContainer.setBarsInsetsListener { insets ->
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    insets.bottom
                )
            }

            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.share -> {
                        post.let(interactionListener::onShare)
                        true
                    }

                    R.id.edit -> {
                        post.let(interactionListener::onEdit)
                        true
                    }

                    R.id.delete -> {
                        post.let(interactionListener::onDelete)
                        true
                    }

                    else -> false
                }
            }
            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
            }

            likers.like.setOnClickListener {
                post.let(interactionListener::onLike)
            }

            commentsAdapter = CommentsAdapter(commentInteractionListener).apply {
                setHasStableIds(true)
            }

            commentListView.adapter = commentsAdapter

            commentLogInText.makeLinks(getString(R.string.link_log_in) to OnClickListener {
                navigateToLogIn()
            }, getString(R.string.link_sign_up) to OnClickListener {
                navigateToSignUp()
            }, textPaintModifier = {
                it.color = it.linkColor
                it.typeface = Typeface.DEFAULT_BOLD
            })

            buttonSendComment.setOnClickListener {
                viewModel.sendComment(commentEditText.text.toString())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        commentsAdapter = null
        super.onDestroyView()
    }

    private fun handleState(state: PostUiState) {
        post = state.post?.also {
            bindPost(it)
            commentsAdapter?.submitList(state.comments)
        } ?: return

        with(binding) {
            toolbar.menu.setGroupVisible(R.id.owned_by_me, post.ownedByMe)
            swipeRefresh.isRefreshing = state.state == State.Loading

            if (state.commentsState == State.Loading) {
                commentsTitle.text = ""
                commentsProgress.show()
            } else {
                val commentCount = state.comments.size
                commentsTitle.text = resources.getQuantityString(
                    R.plurals.comments, commentCount, commentCount
                )
                commentsProgress.hide()
            }

            groupCommentInput.isVisible = state.isLoggedIn
            commentLogInText.isVisible = !state.isLoggedIn

            if (state.isCommentSending) {
                buttonSendComment.isVisible = false
                sendCommentProgress.show()
            } else {
                buttonSendComment.isVisible = true
                sendCommentProgress.hide()
            }
        }

        if (state.state == State.Error) {
            showErrorSnackbar(R.string.error_loading_post) {
                viewModel.refresh()
            }
        }

        if (state.commentsState == State.Error) {
            showErrorSnackbar(R.string.error_loading_comments) {
                viewModel.refreshComments()
            }
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            Event.ErrorLikingPost -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLike()
                }
            }

            Event.ErrorRemovingPost -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.remove()
                }
            }

            Event.PostDeleted -> {
                findNavController().popBackStack()
                lifecycleScope.cancel()
            }

            is Event.ErrorLikingComment -> {
                showErrorSnackbar(R.string.error_liking_comment) {
                    viewModel.toggleCommentLikeById(event.commentId)
                }
            }

            Event.ErrorSendingComment -> {
                showErrorSnackbar(R.string.error_sending_comment)
            }

            is Event.ErrorDeletingComment -> {
                showErrorSnackbar(R.string.error_deleting_comment) {
                    viewModel.deleteCommentById(event.commentId)
                }
            }

            Event.CommentSent -> {
                with(binding) {
                    commentEditText.text?.clear()
                    commentEditText.hideKeyboard()
                    nestedScrollView.smoothScrollTo(0, Int.MAX_VALUE)
                }
                viewModel.refreshComments(false)
            }
        }
    }

    private fun bindPost(post: Post) {
        with(binding) {
            val publishedMilli = post.published?.toEpochMilli() ?: 0

            val publishedDate = TimeUtils.getRelativeDate(
                requireContext(),
                publishedMilli
            )

            published.isVisible = publishedMilli > 0

            author.text = post.author
            published.text = publishedDate
            content.text = post.content

            likers.like.setIconResource(
                if (post.likedByMe) {
                    R.drawable.ic_like
                } else {
                    R.drawable.ic_like_border
                }
            )
            likers.like.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)

            post.authorAvatar?.let {
                avatar.load(it) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.setImageResource(R.drawable.avatar_placeholder)

            post.attachment?.let {
                image.isVisible = it.type == AttachmentType.IMAGE

                when (it.type) {
                    AttachmentType.IMAGE -> {
                        image.load(it.url) {
                            scale(Scale.FILL)
                            crossfade(true)
                        }
                    }
                    AttachmentType.VIDEO -> {}
                    AttachmentType.AUDIO -> {}
                }
            } ?: {
                image.isVisible = false
            }
        }

        binding.content.maxLines = Int.MAX_VALUE
    }

    private fun showErrorSnackbar(
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        action: OnClickListener? = null
    ) {
        snackbar = Snackbar.make(requireView(), resId, duration).apply {
            anchorView = binding.commentInputContainer
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}
