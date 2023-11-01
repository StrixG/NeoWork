package com.obrekht.neowork.posts.ui.post

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
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
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.common.PostInteractionListener
import com.obrekht.neowork.posts.ui.navigateToPostEditor
import com.obrekht.neowork.posts.ui.sharePost
import com.obrekht.neowork.userlist.ui.navigateToUserList
import com.obrekht.neowork.userpreview.ui.UserPreviewClickListener
import com.obrekht.neowork.userpreview.ui.UserPreviewMoreClickListener
import com.obrekht.neowork.users.ui.navigateToUserProfile
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
        override fun onAvatarClick(post: Post) {
            navigateToUserProfile(post.authorId)
        }

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
                showDeleteConfirmation(post.id)
            }
        }
    }

    private val commentInteractionListener = object : CommentInteractionListener {
        override fun onClick(comment: Comment) {
            with(findNavController()) {
                if (currentDestination?.id != R.id.post_fragment) return

                val action = PostFragmentDirections.actionOpenCommentOptions(
                    comment.id, comment.ownedByMe
                )
                navigate(action)
            }
        }

        override fun onLike(comment: Comment) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleCommentLike(comment)
            } else {
                showSuggestAuthDialog()
            }
        }
    }

    private val menuClickListener = Toolbar.OnMenuItemClickListener {
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

    private val userPreviewClickListener: UserPreviewClickListener = { userId ->
        navigateToUserProfile(userId)
    }

    private val likersMoreClickListener: UserPreviewMoreClickListener = {
        navigateToUserList(post.likeOwnerIds, getString(R.string.likers))
    }

    private val mentionedMoreClickListener: UserPreviewMoreClickListener = {
        navigateToUserList(post.likeOwnerIds, getString(R.string.mentioned))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupResultListeners()

        with(binding) {
            commentInputContainer.setBarsInsetsListener { insets ->
                setPadding(
                    paddingLeft, paddingTop, paddingRight,
                    insets.bottom
                )
            }

            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            toolbar.setOnMenuItemClickListener(menuClickListener)
            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
            }

            avatar.setOnClickListener {
                post.let(interactionListener::onAvatarClick)
            }

            likers.buttonLike.setOnClickListener {
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

    private fun setupResultListeners() {
        with(DeleteConfirmationDialogFragment) {
            setFragmentResultListener(
                getRequestKey(DeleteElementType.POST)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    viewModel.delete()
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
                    viewModel.delete()
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
            val publishedMillis = post.published?.toEpochMilli() ?: 0

            val publishedDate = TimeUtils.getRelativeDate(
                requireContext(),
                publishedMillis
            )

            published.isVisible = publishedMillis > 0

            author.text = post.author
            job.text = post.authorJob ?: getString(R.string.open_to_work)
            published.text = publishedDate
            content.text = post.content

            // Likers
            likers.buttonLike.setIconResource(
                if (post.likedByMe) {
                    R.drawable.ic_like
                } else {
                    R.drawable.ic_like_border
                }
            )
            likers.buttonLike.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)

            val likerList = post.users.filterKeys { post.likeOwnerIds.contains(it) }
            likers.preview.setPreviews(likerList)
            likers.preview.setOnPreviewClickListener(userPreviewClickListener)
            likers.preview.setOnMoreClickListener(likersMoreClickListener)

            // Mentioned
            val mentionedCount = post.mentionIds.size
            if (mentionedCount > 0) {
                mentioned.buttonMentioned.text = StringUtils.getCompactNumber(post.mentionIds.size)
            }
            mentioned.root.isVisible = mentionedCount > 0

            val mentionList = post.users.filterKeys { post.mentionIds.contains(it) }
            mentioned.preview.setPreviews(mentionList)
            mentioned.preview.setOnPreviewClickListener(userPreviewClickListener)
            mentioned.preview.setOnMoreClickListener(mentionedMoreClickListener)

            // Avatar
            post.authorAvatar?.let {
                avatar.load(it) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.setImageResource(R.drawable.avatar_placeholder)

            // Attachments
            post.attachment?.let {
                image.isVisible = false
                buttonPlayVideo.isVisible = false

                when (it.type) {
                    AttachmentType.IMAGE -> {
                        image.load(it.url) {
                            crossfade(true)
                        }
                        image.isVisible = true
                    }

                    AttachmentType.VIDEO -> {
                        image.load(it.url) {
                            crossfade(true)
                            listener { _, _ ->
                                buttonPlayVideo.isVisible = true
                            }
                        }
                        image.isVisible = true
                    }

                    AttachmentType.AUDIO -> {}
                }
            } ?: run {
                image.isVisible = false
                buttonPlayVideo.isVisible = false
            }
        }

        binding.content.maxLines = Int.MAX_VALUE
    }

    private fun showDeleteConfirmation(postId: Long) {
        val action = PostFragmentDirections.actionOpenDeleteConfirmation(
            postId,
            DeleteElementType.POST
        )
        findNavController().navigate(action)
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
