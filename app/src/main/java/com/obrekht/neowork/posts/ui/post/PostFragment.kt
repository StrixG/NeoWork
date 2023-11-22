package com.obrekht.neowork.posts.ui.post

import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
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
import com.obrekht.neowork.media.ui.navigateToMediaView
import com.obrekht.neowork.media.usecase.retrieveMediaMetadata
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
import com.obrekht.neowork.utils.isLightTheme
import com.obrekht.neowork.utils.makeLinks
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setAllOnClickListener
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val LOCATION_PREVIEW_DEFAULT_ZOOM = 15f

@AndroidEntryPoint
class PostFragment : Fragment(R.layout.fragment_post) {
    private val binding by viewBinding(FragmentPostBinding::bind)
    private val viewModel: PostViewModel
            by hiltNavGraphViewModels(R.id.post_graph)

    private lateinit var post: Post
    private var snackbar: Snackbar? = null
    private var commentsAdapter: CommentsAdapter? = null

    private val searchManager = SearchFactory.getInstance()
        .createSearchManager(SearchManagerType.ONLINE)

    private val searchOptions = SearchOptions().apply {
        searchTypes = SearchType.GEO.value
        resultPageSize = 1
    }

    private val searchListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val geoObject = response.collection.children.firstOrNull()?.obj
            binding.locationAddress.text = geoObject?.let {
                "${geoObject.name}\n${geoObject.descriptionText}"
            } ?: getString(R.string.location_address_unknown)
        }

        override fun onSearchError(error: Error) {
            binding.locationAddress.text = getString(R.string.location_address_unknown)
        }
    }

    private val interactionListener = object : PostInteractionListener {
        override fun onAvatarClick(post: Post) {
            navigateToUserProfile(post.authorId)
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
            return if (viewModel.isLoggedIn) {
                viewModel.toggleLike()
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
        navigateToUserList(post.mentionIds, getString(R.string.mentioned))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
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
                interactionListener.onAvatarClick(post)
            }

            attachmentPreview.setOnClickListener {
                interactionListener.onAttachmentClick(post, attachmentPreview)
            }

            buttonPlayAudio.setOnClickListener {
                interactionListener.onPlayAudioButtonClick(post)
            }

            likers.setButtonClickListener {
                interactionListener.onLike(post)
            }
            likers.preview.setOnPreviewClickListener(userPreviewClickListener)
            likers.preview.setOnMoreClickListener(likersMoreClickListener)

            mentioned.setButtonClickListener {
                mentionedMoreClickListener()
            }
            mentioned.preview.setOnPreviewClickListener(userPreviewClickListener)
            mentioned.preview.setOnMoreClickListener(mentionedMoreClickListener)

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
        binding.commentLogInText.text = null

        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.locationPreview.onStart()
    }

    override fun onStop() {
        binding.locationPreview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
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
        post = state.post ?: return

        bindPost(post)
        commentsAdapter?.submitList(state.comments)

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
            updateAudioPlayButton(state.isAudioPlaying)
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

    private fun handleEvent(event: UiEvent) {
        when (event) {
            UiEvent.ErrorLikingPost -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLike()
                }
            }

            UiEvent.ErrorRemovingPost -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.delete()
                }
            }

            UiEvent.PostDeleted -> {
                findNavController().popBackStack()
                lifecycleScope.cancel()
            }

            is UiEvent.ErrorLikingComment -> {
                showErrorSnackbar(R.string.error_liking_comment) {
                    viewModel.toggleCommentLikeById(event.commentId)
                }
            }

            UiEvent.ErrorSendingComment -> {
                showErrorSnackbar(R.string.error_sending_comment)
            }

            is UiEvent.ErrorDeletingComment -> {
                showErrorSnackbar(R.string.error_deleting_comment) {
                    viewModel.deleteCommentById(event.commentId)
                }
            }

            UiEvent.CommentSent -> {
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

            // Avatar
            post.authorAvatar?.let {
                avatar.load(it) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            // Likers
            likers.button.isChecked = post.likedByMe
            likers.button.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)

            val likerList = post.users.filterKeys { post.likeOwnerIds.contains(it) }
            likers.preview.setPreviews(likerList)

            // Mentioned
            val mentionedCount = post.mentionIds.size
            if (mentionedCount > 0) {
                val mentionList = post.users.filterKeys { post.mentionIds.contains(it) }
                mentioned.preview.setPreviews(mentionList)
                mentioned.button.text = StringUtils.getCompactNumber(mentionedCount)
            }
            mentioned.isVisible = mentionedCount > 0

            // Location
            post.coords?.let { (latitude, longitude) ->
                searchManager.submit(
                    Point(latitude, longitude),
                    LOCATION_PREVIEW_DEFAULT_ZOOM.toInt(),
                    searchOptions,
                    searchListener
                )

                locationPreviewGroup.setAllOnClickListener {
                    openMap(latitude, longitude)
                }
                locationPreview.setNoninteractive(true)
                locationPreview.mapWindow.map.apply {
                    val cameraPosition = CameraPosition(
                        Point(latitude, longitude),
                        LOCATION_PREVIEW_DEFAULT_ZOOM,
                        0f, 0f
                    )
                    move(cameraPosition)
                    isNightModeEnabled = !resources.configuration.isLightTheme
                }
                locationAddress.text = getString(R.string.loading)

                val pinSize = resources.getDimension(R.dimen.pin_size_preview)
                pin.translationY = -pinSize / 2

                locationPreviewGroup.isVisible = true
            } ?: run {
                locationPreviewGroup.isVisible = false
            }

            // Attachments
            attachmentPreview.isVisible = false
            buttonPlayVideo.isVisible = false
            audioGroup.isVisible = false

            post.attachment?.let {
                attachmentPreview.transitionName = it.url

                when (it.type) {
                    AttachmentType.IMAGE -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                            listener { _, _ ->
                                startPostponedEnterTransition()
                            }
                        }
                        attachmentPreview.isVisible = true
                    }

                    AttachmentType.VIDEO -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                            listener { _, _ ->
                                buttonPlayVideo.isVisible = true
                                startPostponedEnterTransition()
                            }
                        }
                        attachmentPreview.isVisible = true
                    }

                    AttachmentType.AUDIO -> {
                        audioGroup.isVisible = true
                        audioTitle.setText(R.string.loading)
                        audioArtist.text = null

                        viewLifecycleScope.launch {
                            val context = audioGroup.context
                            val mediaItem = MediaItem.fromUri(it.url)

                            val mediaMetadata = mediaItem.retrieveMediaMetadata(context)
                            mediaMetadata?.let {
                                audioTitle.text = mediaMetadata.title
                                    ?: context.getString(R.string.audio_untitled)
                                audioArtist.text = mediaMetadata.artist
                                    ?: context.getString(R.string.audio_unknown_artist)
                            } ?: run {
                                audioTitle.text = context.getString(R.string.audio_unknown)
                            }
                        }
                        startPostponedEnterTransition()
                    }
                }
            } ?: startPostponedEnterTransition()
        }

        binding.content.maxLines = Int.MAX_VALUE
    }

    private fun openMap(latitude: Double, longitude: Double) {
        val intent = Intent(
            ACTION_VIEW,
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        )
        startActivity(intent)
    }

    private fun updateAudioPlayButton(isPlaying: Boolean) {
        val iconId = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }
        binding.buttonPlayAudio.setIconResource(iconId)
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
