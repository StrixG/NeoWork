package com.obrekht.neowork.core.ui.mainscreen

import androidx.lifecycle.ViewModel
import com.obrekht.neowork.auth.data.local.AppAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val appAuth: AppAuth
) : ViewModel() {

    val loggedInState = appAuth.loggedInState
    val loggedInUserId: Long
        get() = appAuth.state.value.id

    fun logOut() {
        appAuth.removeAuth()
    }
}
