package com.obrekht.neowork.userchooser.ui

import androidx.fragment.app.Fragment
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.editor.ui.editor.EditorFragmentDirections

fun Fragment.navigateToUserChooser(requestKey: String, userIds: LongArray) {
    val action = EditorFragmentDirections.actionOpenUserChooser(requestKey, userIds)
    findRootNavController().navigate(action)
}
