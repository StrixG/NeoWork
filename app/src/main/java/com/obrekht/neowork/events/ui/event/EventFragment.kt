package com.obrekht.neowork.events.ui.event

import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
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
import com.obrekht.neowork.auth.ui.showSuggestAuthDialog
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentEventBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.model.EventType
import com.obrekht.neowork.events.ui.common.EventInteractionListener
import com.obrekht.neowork.events.ui.navigateToEventEditor
import com.obrekht.neowork.events.ui.shareEvent
import com.obrekht.neowork.posts.ui.post.PostFragmentDirections
import com.obrekht.neowork.userlist.ui.navigateToUserList
import com.obrekht.neowork.userpreview.ui.UserPreviewClickListener
import com.obrekht.neowork.userpreview.ui.UserPreviewMoreClickListener
import com.obrekht.neowork.users.ui.navigateToUserProfile
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import com.obrekht.neowork.utils.isLightTheme
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setAllOnClickListener
import com.obrekht.neowork.utils.viewBinding
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
class EventFragment : Fragment(R.layout.fragment_event) {
    private val binding by viewBinding(FragmentEventBinding::bind)
    private val viewModel: EventViewModel
            by hiltNavGraphViewModels(R.id.event_fragment)

    private lateinit var event: Event
    private var snackbar: Snackbar? = null

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

    private val interactionListener = object : EventInteractionListener {
        override fun onAvatarClick(event: Event) {
            navigateToUserProfile(event.authorId)
        }

        override fun onLike(event: Event) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleLike()
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onShare(event: Event) {
            shareEvent(event)
        }

        override fun onParticipate(event: Event) {
            if (viewModel.isLoggedIn) {
                viewModel.toggleParticipation()
            } else {
                showSuggestAuthDialog()
            }
        }

        override fun onEdit(event: Event) {
            if (viewModel.isLoggedIn) {
                navigateToEventEditor(event.id)
            }
        }

        override fun onDelete(event: Event) {
            if (viewModel.isLoggedIn) {
                showDeleteConfirmation(event.id)
            }
        }
    }

    private val menuClickListener = Toolbar.OnMenuItemClickListener {
        when (it.itemId) {
            R.id.share -> {
                event.let(interactionListener::onShare)
                true
            }

            R.id.edit -> {
                event.let(interactionListener::onEdit)
                true
            }

            R.id.delete -> {
                event.let(interactionListener::onDelete)
                true
            }

            else -> false
        }
    }

    private val userPreviewClickListener: UserPreviewClickListener = { userId ->
        navigateToUserProfile(userId)
    }

    private val likersMoreClickListener: UserPreviewMoreClickListener = {
        navigateToUserList(event.likeOwnerIds, getString(R.string.likers))
    }

    private val participantsMoreClickListener: UserPreviewMoreClickListener = {
        navigateToUserList(event.participantsIds, getString(R.string.mentioned))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupResultListeners()

        with(binding) {
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
                event.let(interactionListener::onAvatarClick)
            }

            likers.setButtonClickListener() {
                event.let(interactionListener::onLike)
            }
            likers.preview.setOnPreviewClickListener(userPreviewClickListener)
            likers.preview.setOnMoreClickListener(likersMoreClickListener)

            participants.setButtonClickListener() {
                event.let(interactionListener::onParticipate)
            }
            participants.preview.setOnPreviewClickListener(userPreviewClickListener)
            participants.preview.setOnMoreClickListener(participantsMoreClickListener)
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
                getRequestKey(DeleteElementType.EVENT)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    viewModel.delete()
                }
            }
        }
    }

    private fun handleState(state: EventUiState) {
        event = state.event?.also {
            bindEvent(it)
        } ?: return

        with(binding) {
            toolbar.menu.setGroupVisible(R.id.owned_by_me, event.ownedByMe)
            swipeRefresh.isRefreshing = state.state == State.Loading
        }

        if (state.state == State.Error) {
            showErrorSnackbar(R.string.error_loading_post) {
                viewModel.refresh()
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            UiEvent.ErrorLikingEvent -> {
                showErrorSnackbar(R.string.error_liking) {
                    viewModel.toggleLike()
                }
            }

            UiEvent.ErrorRemovingEvent -> {
                showErrorSnackbar(R.string.error_deleting) {
                    viewModel.delete()
                }
            }

            UiEvent.ErrorParticipatingEvent -> {
                showErrorSnackbar(R.string.error_participating) {
                    viewModel.toggleParticipation()
                }
            }

            UiEvent.EventDeleted -> {
                findNavController().popBackStack()
                lifecycleScope.cancel()
            }
        }
    }

    private fun bindEvent(event: Event) {
        with(binding) {
            val publishedMillis = event.published?.toEpochMilli() ?: 0

            val publishedDate = TimeUtils.getRelativeDate(
                requireContext(),
                publishedMillis
            )

            published.isVisible = publishedMillis > 0

            author.text = event.author
            job.text = event.authorJob ?: getString(R.string.open_to_work)
            published.text = publishedDate
            type.setText(when(event.type) {
                EventType.OFFLINE -> R.string.event_type_offline
                EventType.ONLINE -> R.string.event_type_online
            })
            val dateMillis = event.datetime?.toEpochMilli() ?: 0
            date.text = TimeUtils.getRelativeDate(
                requireContext(),
                dateMillis
            )
            content.text = event.content

            // Avatar
            event.authorAvatar?.let {
                avatar.load(it) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            // Likers
            likers.button.setIconResource(
                if (event.likedByMe) {
                    R.drawable.ic_like
                } else {
                    R.drawable.ic_like_border
                }
            )
            likers.button.text = StringUtils.getCompactNumber(event.likeOwnerIds.size)

            val likerList = event.users.filterKeys { event.likeOwnerIds.contains(it) }
            likers.preview.setPreviews(likerList)

            // Participants
            participants.button.setIconResource(
                if (event.participatedByMe) {
                    R.drawable.ic_person_check
                } else {
                    R.drawable.ic_people_outline
                }
            )
            participants.button.text = StringUtils.getCompactNumber(event.participantsIds.size)

            val participantList = event.users.filterKeys { event.participantsIds.contains(it) }
            participants.setUserPreviews(participantList)

            // Location
            event.coords?.let { (latitude, longitude) ->
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

            event.attachment?.let {
                when (it.type) {
                    AttachmentType.IMAGE -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                        }
                        attachmentPreview.isVisible = true
                    }

                    AttachmentType.VIDEO -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                            listener { _, _ ->
                                buttonPlayVideo.isVisible = true
                            }
                        }
                        attachmentPreview.isVisible = true
                    }

                    AttachmentType.AUDIO -> {}
                }
            }
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
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}
