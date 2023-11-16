package com.obrekht.neowork.posts.ui.common

import android.widget.ImageView
import com.obrekht.neowork.posts.model.Post

interface PostInteractionListener {
    fun onClick(post: Post) {}
    fun onAvatarClick(post: Post) {}
    fun onAttachmentClick(post: Post, view: ImageView) {}
    fun onPlayAudioButtonClick(post: Post) {}
    fun onLike(post: Post): Boolean = false
    fun onShare(post: Post) {}
    fun onEdit(post: Post) {}
    fun onDelete(post: Post) {}
}
