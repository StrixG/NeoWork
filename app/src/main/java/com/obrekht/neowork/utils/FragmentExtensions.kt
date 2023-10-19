package com.obrekht.neowork.utils

import androidx.fragment.app.Fragment

inline fun <reified T : Fragment> Fragment.findParent(): Fragment? {
    var fragment: Fragment? = this
    while (fragment != null) {
        if (fragment is T) {
            return fragment
        }
        fragment = fragment.parentFragment
    }

    return null
}