package com.obrekht.neowork.jobs.ui.addedit

import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.core.ui.findRootNavController

fun Fragment.navigateToJobEditor(jobId: Long = 0) {
    val action = NavGraphDirections.actionOpenJobEditor(jobId)
    findRootNavController().navigate(action)
}
