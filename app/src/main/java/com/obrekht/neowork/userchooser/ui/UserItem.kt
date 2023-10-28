package com.obrekht.neowork.userchooser.ui

import com.obrekht.neowork.users.model.User

data class UserItem(
    val isSelected: Boolean,
    val user: User
)
