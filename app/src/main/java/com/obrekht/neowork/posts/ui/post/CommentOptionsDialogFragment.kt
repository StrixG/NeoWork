package com.obrekht.neowork.posts.ui.post

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.BottomSheetCommentOptionsBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.posts.ui.navigateToCommentEditor
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommentOptionsDialogFragment :
    BottomSheetDialogFragment(R.layout.bottom_sheet_comment_options) {

    private val viewModel: PostViewModel
            by hiltNavGraphViewModels(R.id.post_graph)

    private val binding by viewBinding(BottomSheetCommentOptionsBinding::bind)
    private val args: CommentOptionsDialogFragmentArgs by navArgs()

    private var comment: Comment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(DeleteConfirmationDialogFragment) {
            setFragmentResultListener(
                getRequestKey(DeleteElementType.COMMENT)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    val commentId = bundle.getLong(RESULT_ELEMENT_ID)
                    viewModel.deleteCommentById(commentId)
                    dismiss()
                }
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.getComment(args.commentId).onEach {
                    // Close options dialog if the comment was deleted
                    if (it == null) {
                        dismiss()
                    } else {
                        comment = it
                    }
                }.launchIn(this)
            }
        }

        with(binding) {
            val adapter = CommentOptionsAdapter({})
            optionList.adapter = adapter

            // Copy
            adapter.addItem(CommentOption(R.drawable.ic_copy, R.string.copy_comment) {
                comment?.let {
                    val clipboardManager =
                        requireContext().getSystemService(Service.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(it.author, it.content)
                    clipboardManager.setPrimaryClip(clip)

                    Toast.makeText(requireContext(), R.string.comment_copied, Toast.LENGTH_SHORT)
                        .show()
                }
                dismiss()
            })

            if (viewModel.isLoggedIn && args.ownedByMe) {
                adapter.addItems(
                    // Edit
                    CommentOption(R.drawable.ic_edit_24, R.string.edit_comment) {
                        navigateToCommentEditor(args.commentId)
                        dismiss()
                    },
                    // Delete
                    CommentOption(R.drawable.ic_delete, R.string.delete_comment) {
                        val action = CommentOptionsDialogFragmentDirections.actionOpenDeleteConfirmation(
                            args.commentId,
                            DeleteElementType.COMMENT
                        )
                        findNavController().navigate(action)
                    })
            }
        }
    }
}
