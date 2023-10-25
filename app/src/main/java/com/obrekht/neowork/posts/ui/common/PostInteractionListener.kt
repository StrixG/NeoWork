package com.obrekht.neowork.posts.ui.common

import com.obrekht.neowork.posts.model.Post

interface PostInteractionListener {
    fun onClick(post: Post) {}
    fun onAvatarClick(post: Post) {}
    fun onLike(post: Post) {}
    fun onShare(post: Post) {}
    fun onEdit(post: Post) {}
    fun onDelete(post: Post) {}
}