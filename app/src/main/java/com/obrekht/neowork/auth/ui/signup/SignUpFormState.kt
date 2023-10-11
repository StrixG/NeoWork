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

enum class PasswordConfirmationError : SignUpFormError {
    DoNotMatch
}

data class SignUpFormState(
    val nameError: NameError? = null,
    val usernameError: UsernameError? = null,
    val passwordError: PasswordError? = null,
    val passwordConfirmationError: PasswordConfirmationError? = null,
    val isDataValid: Boolean = false
)
