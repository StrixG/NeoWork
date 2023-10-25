package com.obrekht.neowork.jobs.ui.addedit

data class AddEditJobFormState(
    val name: String = "",
    val position: String = "",
    val link: String = "",
    val nameError: NameError? = null,
    val positionError: PositionError? = null,
    val linkError: LinkError? = null,
    val isDataValid: Boolean = false
)

sealed interface AddEditJobFormError

enum class NameError : AddEditJobFormError {
    Empty
}

enum class PositionError : AddEditJobFormError {
    Empty
}

enum class LinkError : AddEditJobFormError {
    InvalidLink
}