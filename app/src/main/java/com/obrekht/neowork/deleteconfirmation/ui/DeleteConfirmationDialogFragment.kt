package com.obrekht.neowork.deleteconfirmation.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obrekht.neowork.R

class DeleteConfirmationDialogFragment : DialogFragment() {
    private val args: DeleteConfirmationDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val clickListener = DialogInterface.OnClickListener { _, which ->
            setFragmentResult(
                getRequestKey(args.elementType), bundleOf(
                    RESULT_CLICKED_BUTTON to which,
                    RESULT_ELEMENT_ID to args.elementId
                )
            )
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_post_confirmation_title))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setNegativeButton(getString(R.string.cancel), clickListener)
            .setPositiveButton(getString(R.string.delete), clickListener).show()
    }

    companion object {
        const val RESULT_CLICKED_BUTTON = "clickedButton"
        const val RESULT_ELEMENT_ID = "elementId"

        fun getRequestKey(elementType: DeleteElementType) = "deleteConfirmation_$elementType"
    }
}

enum class DeleteElementType {
    POST,
    COMMENT,
    JOB
}
