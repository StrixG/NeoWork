package com.obrekht.neowork.auth.ui.signup

interface SignUpFormError

enum class NameError : SignUpFormError {
    Empty
}

enum class UsernameError : SignUpFormError {
    Empty
}

enum class PasswordError : SignUpFormError {
    Empty
}

data class SignUpFormState(
    val nameError: NameError? = null,
    val usernameError: UsernameError? = null,
    val passwordError: PasswordError? = null,
    val isDataValid: Boolean = false
)
