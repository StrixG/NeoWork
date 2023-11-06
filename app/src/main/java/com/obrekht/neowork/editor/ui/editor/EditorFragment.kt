package com.obrekht.neowork.editor.ui.editor

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.core.model.Coordinates
import com.obrekht.neowork.databinding.FragmentEditorBinding
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.map.navigateToLocationPicker
import com.obrekht.neowork.map.ui.LocationPickerFragment
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.userchooser.ui.UserChooserFragment
import com.obrekht.neowork.userchooser.ui.navigateToUserChooser
import com.obrekht.neowork.utils.focusAndShowKeyboard
import com.obrekht.neowork.utils.hideKeyboard
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val IMAGE_COMPRESS_SIZE = 2048
private const val LOCATION_PREVIEW_DEFAULT_ZOOM = 15f

@AndroidEntryPoint
class EditorFragment : Fragment(R.layout.fragment_editor) {
    private val binding by viewBinding(FragmentEditorBinding::bind)
    private val viewModel: EditorViewModel
            by hiltNavGraphViewModels(R.id.editor_fragment)
    private val args: EditorFragmentArgs by navArgs()

    private val editableConfig by lazy {
        EditableConfig.getByType(args.editableType)
    }

    private val uiState: EditorUiState
        get() = viewModel.uiState.value

    private val searchManager: SearchManager = SearchFactory.getInstance()
        .createSearchManager(SearchManagerType.ONLINE)

    private val searchOptions = SearchOptions().apply {
        searchTypes = SearchType.GEO.value
        resultPageSize = 1
    }

    private val searchSessionListener = object : Session.SearchListener {
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

    private val takePhotoResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    it.data?.data?.let(viewModel::setAttachment)
                }

                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(it.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val attachMediaLauncher =
        registerForActivityResult(GetMediaContract()) { uri ->
            uri?.let(viewModel::setAttachment)
        }

    private val menuClickListener = Toolbar.OnMenuItemClickListener { menuItem ->
        with(binding) {
            when (menuItem.itemId) {
                R.id.save -> {
                    menuItem.isVisible = false
                    progressBar.isVisible = true
                    inputField.isEnabled = false

                    val text = inputField.text.toString()
                    viewModel.save(text)
                    inputField.hideKeyboard()

                    true
                }

                else -> false
            }
        }
    }

    private val bottomAppBarClickListener = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.take_picture -> {
                openCamera()
                true
            }

            R.id.attach_media -> {
                openAttachmentPicker()
                true
            }

            R.id.add_users -> {
                when (args.editableType) {
                    EditableType.POST -> {
                        viewModel.navigateToUserChooser(UserChooserCategory.MENTIONS)
                        true
                    }

                    EditableType.EVENT -> {
                        viewModel.navigateToUserChooser(UserChooserCategory.SPEAKERS)
                        true
                    }

                    else -> false
                }
            }

            R.id.add_location -> {
                viewModel.navigateToLocationPicker()
                true
            }

            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            tryExit()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        setFragmentResultListener(UserChooserCategory.MENTIONS.requestKey) { _, bundle ->
            bundle.getLongArray(UserChooserFragment.RESULT_CHOSEN_USER_IDS)?.let { chosenUserIds ->
                viewModel.setChosenUserIds(chosenUserIds.toSet())
            }
        }

        setFragmentResultListener(UserChooserCategory.SPEAKERS.requestKey) { _, bundle ->
            bundle.getLongArray(UserChooserFragment.RESULT_CHOSEN_USER_IDS)?.let { chosenUserIds ->
                viewModel.setChosenUserIds(chosenUserIds.toSet())
            }
        }

        with(LocationPickerFragment) {
            setFragmentResultListener(REQUEST_KEY) { _, bundle ->
                val latitude = bundle.getDouble(RESULT_LOCATION_LATITUDE)
                val longitude = bundle.getDouble(RESULT_LOCATION_LONGITUDE)
                viewModel.setLocationCoordinates(Coordinates(latitude, longitude))
            }
        }

        val scrollViewBottomMargin = if (editableConfig.doesSupportAttachments) {
            editScrollView.marginBottom
        } else 0

        editScrollView.setBarsInsetsListener {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it.bottom + scrollViewBottomMargin
            }
        }

        appBar.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        toolbar.apply {
            setNavigationOnClickListener {
                tryExit(true)
            }
            setOnMenuItemClickListener(menuClickListener)
        }

        inputField.setHint(editableConfig.hintInputField)

        if (editableConfig.doesSupportAttachments) {
            bottomAppBar.setOnMenuItemClickListener(bottomAppBarClickListener)

            buttonRemoveAttachment.setOnClickListener {
                viewModel.removeAttachment()
            }
            buttonRemoveChosenUsers.setOnClickListener {
                viewModel.setChosenUserIds(emptySet())
            }
            buttonRemoveLocation.setOnClickListener {
                viewModel.setLocationCoordinates(null)
            }

            if (args.editableType == EditableType.EVENT) {
                buttonEventOptions.setOnClickListener {
                    val action = EditorFragmentDirections.actionOpenEventOptions()
                    findNavController().navigate(action)
                }
                buttonEventOptions.isVisible = true
            } else {
                buttonEventOptions.isVisible = false
            }
        }
        bottomAppBar.isVisible = editableConfig.doesSupportAttachments

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.attachment.onEach { attachment ->
                    attachmentPreviewGroup.isVisible = false

                    attachment?.let {
                        val uri = attachment.uri ?: attachment.file?.toUri()
                        when (attachment.type) {
                            AttachmentType.IMAGE -> {
                                attachmentPreview.load(uri) {
                                    crossfade(true)
                                }
                                attachmentPreviewGroup.isVisible = true
                                buttonPlayVideo.isVisible = false
                            }

                            AttachmentType.VIDEO -> {
                                attachmentPreview.load(uri) {
                                    crossfade(true)
                                    listener { _, _ ->
                                        buttonPlayVideo.isVisible = true
                                    }
                                }
                                attachmentPreviewGroup.isVisible = true
                            }

                            AttachmentType.AUDIO -> {}
                        }
                    }
                }.launchIn(this)

                viewModel.edited.onEach { editable ->
                    when (editable) {
                        is Post -> {
                            val count = editable.mentionIds.size
                            chosenUsersGroup.isVisible = count > 0
                            chosenUsers.text = resources.getQuantityString(
                                R.plurals.mentions, count, count
                            )
                        }

                        is Event -> {
                            val count = editable.speakerIds.size
                            chosenUsersGroup.isVisible = count > 0
                            chosenUsers.text = resources.getQuantityString(
                                R.plurals.speakers, count, count
                            )
                        }
                    }

                    val coordinates = when (editable) {
                        is Post -> editable.coords
                        is Event -> editable.coords
                        else -> null
                    }
                    locationPreviewGroup.isVisible = (coordinates != null)
                    coordinates?.let { (latitude, longitude) ->
                        locationAddress.text = getString(R.string.loading)
                        searchManager.submit(
                            Point(latitude, longitude),
                            LOCATION_PREVIEW_DEFAULT_ZOOM.toInt(),
                            searchOptions,
                            searchSessionListener
                        )
                    }
                }.launchIn(this)

                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }

        Unit
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun handleState(state: EditorUiState) = with(binding) {
        if (state.shouldInitialize) {
            inputField.isEnabled = true
            inputField.append(args.text.ifEmpty {
                state.initialContent
            })
            viewModel.onInitialized()

            inputField.focusAndShowKeyboard()
        } else if (state.initialized) {
            inputField.isEnabled = true
        }

        Unit
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.NavigateToUserChooser -> {
                navigateToUserChooser(event.category.requestKey, event.userIds)
            }

            is UiEvent.NavigateToLocationPicker -> {
                navigateToLocationPicker(event.locationPoint)
            }

            is UiEvent.Saved -> {
                with(findNavController()) {
                    previousBackStackEntry?.savedStateHandle?.set(KEY_SCROLL_TO_ID, args.id)
                    popBackStack()
                }
            }

            else -> {
                val message = when (event) {
                    UiEvent.ErrorEmptyContent -> editableConfig.messageErrorEmpty
                    UiEvent.ErrorSaving -> editableConfig.messageErrorSaving
                    else -> editableConfig.messageErrorLoading
                }
                with(binding) {
                    Snackbar.make(inputField, message, Snackbar.LENGTH_LONG)
                        .setAnchorView(bottomAppBar)
                        .show()

                    progressBar.isVisible = false
                    inputField.isEnabled = true
                    toolbar.menu.findItem(R.id.save).isVisible = true
                }
            }
        }
    }

    private fun openCamera() {
        ImagePicker.with(this@EditorFragment)
            .crop()
            .compress(IMAGE_COMPRESS_SIZE)
            .provider(ImageProvider.CAMERA)
            .createIntent(takePhotoResultLauncher::launch)
    }

    private fun openAttachmentPicker() {
        attachMediaLauncher.launch("*/*")
    }

    private fun tryExit(shouldNavigateUp: Boolean = false) {
        val text = binding.inputField.text.toString()
        if (text.isNotBlank() && text != uiState.initialContent) {
            showExitConfirmation(shouldNavigateUp)
        } else {
            cancelEditAndExit(shouldNavigateUp)
        }
    }

    private fun showExitConfirmation(shouldNavigateUp: Boolean = false) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cancel_edit_title))
            .setMessage(getString(R.string.cancel_edit_message))
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.dont_save)) { _, _ ->
                cancelEditAndExit(shouldNavigateUp)
            }
            .show()
    }

    private fun cancelEditAndExit(shouldNavigateUp: Boolean = false) {
        with(findNavController()) {
            if (shouldNavigateUp) {
                navigateUp()
            } else {
                popBackStack()
            }
        }
    }

    companion object {
        const val KEY_SCROLL_TO_ID = "scrollToId"
    }
}
