package com.obrekht.neowork.auth.ui.login

interface LoginFormError

enum class UsernameError : LoginFormError {
    Empty
}

enum class PasswordError : LoginFormError {
    Empty
}

data class LoginFormState(
    val usernameError: UsernameError? = null,
    val passwordError: PasswordError? = null,
    val isDataValid: Boolean = false
)
