package com.obrekht.neowork.users.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.obrekht.neowork.users.model.User

@Entity("user")
data class UserEntity(
    @PrimaryKey
    val userId: Long,
    val login: String,
    val name: String,
    val avatar: String? = null
)

fun UserEntity.toModel() = User(userId, login, name, avatar)
fun User.toEntity() = UserEntity(id, login, name, avatar)

fun List<UserEntity>.toModel() = map(UserEntity::toModel)
fun List<User>.toEntity() = map(User::toEntity)