package com.obrekht.neowork.users.ui

import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.core.ui.findRootNavController

fun Fragment.navigateToUserProfile(userId: Long) {
    val action = NavGraphDirections.actionOpenUserProfile(userId)
    findRootNavController().navigate(action)
}
