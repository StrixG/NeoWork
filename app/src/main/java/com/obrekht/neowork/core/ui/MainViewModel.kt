package com.obrekht.neowork.core.ui

import androidx.lifecycle.ViewModel
import com.obrekht.neowork.auth.data.local.AppAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appAuth: AppAuth
) : ViewModel() {

    val loggedInState = appAuth.loggedInState

    fun logOut() {
        appAuth.removeAuth()
    }
}