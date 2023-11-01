package com.obrekht.neowork.userlist.ui

import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.core.ui.findRootNavController

fun Fragment.navigateToUserList(userIds: Collection<Long>, title: String) {
    val action = NavGraphDirections.actionOpenUserList(userIds.toLongArray(), title)
    findRootNavController().navigate(action)
}
