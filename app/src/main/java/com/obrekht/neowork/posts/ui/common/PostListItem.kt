package com.obrekht.neowork.posts.ui.common

import com.obrekht.neowork.posts.model.Post
import java.time.LocalDate

sealed interface PostListItem

data class PostItem(
    val post: Post = Post(),
    val isAudioPlaying: Boolean = false
) : PostListItem

data class DateSeparatorItem(val date: LocalDate) : PostListItem
