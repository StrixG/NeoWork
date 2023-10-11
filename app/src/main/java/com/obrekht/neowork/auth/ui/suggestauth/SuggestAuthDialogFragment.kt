package com.obrekht.neowork.auth.ui.suggestauth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obrekht.neowork.R

class SuggestAuthDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val clickListener = DialogInterface.OnClickListener { _, which ->
            setFragmentResult(
                REQUEST_KEY, bundleOf(
                    RESULT_POSITIVE to (which == DialogInterface.BUTTON_POSITIVE),
                )
            )
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.suggest_auth_title))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.log_in), clickListener)
            .show()
    }

    companion object {
        const val REQUEST_KEY = "suggestAuth"
        const val RESULT_POSITIVE = "positive"
    }
}
