package com.obrekht.neowork.posts.ui.feed

import com.obrekht.neowork.posts.model.Post
import java.time.LocalDate

sealed interface FeedItem

data class PostItem(val post: Post) : FeedItem
data class DateSeparatorItem(val date: LocalDate) : FeedItem