package com.obrekht.neowork.auth.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.R
import com.obrekht.neowork.core.ui.findRootNavController

fun Fragment.navigateToLogIn() {
    findRootNavController().navigate(NavGraphDirections.actionOpenLogIn())
}

fun Fragment.navigateToSignUp() {
    findRootNavController().navigate(NavGraphDirections.actionOpenSignUp())
}

fun Fragment.showSuggestAuthDialog() {
    findNavController().navigate(R.id.suggest_auth_dialog)
}
