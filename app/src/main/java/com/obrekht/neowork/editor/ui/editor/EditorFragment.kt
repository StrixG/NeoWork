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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentEditorBinding
import com.obrekht.neowork.utils.focusAndShowKeyboard
import com.obrekht.neowork.utils.hideKeyboard
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditorFragment : Fragment(R.layout.fragment_editor) {
    private val binding by viewBinding(FragmentEditorBinding::bind)
    private val viewModel: EditorViewModel by viewModels()
    private val args: EditorFragmentArgs by navArgs()

    private val editableConfig by lazy {
        EditableConfig.getByType(args.editableType)
    }

    private val uiState: EditorUiState
        get() = viewModel.uiState.value

    private val takePhotoResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    it.data?.data?.let(viewModel::changeAttachment)
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
            uri?.let(viewModel::changeAttachment)
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

            R.id.add_mentions -> {
                // TODO: Open user chooser
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
        }
        bottomAppBar.isVisible = editableConfig.doesSupportAttachments

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.attachment.onEach {
                    if (it != null) {
                        attachmentPreviewGroup.isVisible = true
                        attachmentPhoto.isVisible = it.type == AttachmentType.IMAGE

                        if (it.type == AttachmentType.IMAGE) {
                            if (it.uri != null) {
                                attachmentPhoto.load(it.uri)
                            } else if (it.file != null) {
                                attachmentPhoto.setImageURI(it.file.toUri())
                            }
                        }
                    } else {
                        attachmentPreviewGroup.isVisible = false
                    }
                }.launchIn(this)

                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }

        Unit
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

    private fun handleEvent(event: Event) {
        if (event == Event.Saved) {
            with(findNavController()) {
                previousBackStackEntry?.savedStateHandle?.set(KEY_SCROLL_TO_ID, args.id)
                popBackStack()
            }
        } else {
            val message = when (event) {
                Event.ErrorEmptyContent -> editableConfig.messageErrorEmpty
                Event.ErrorSaving -> editableConfig.messageErrorSaving
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

    private fun openCamera() {
        ImagePicker.with(this@EditorFragment)
            .crop()
            .compress(2048)
//            .provider(ImageProvider.CAMERA)
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