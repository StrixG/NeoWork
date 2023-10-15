package com.obrekht.neowork.posts.ui.post

import com.obrekht.neowork.posts.model.Comment

interface CommentInteractionListener {
    fun onClick(comment: Comment) {}
    fun onLike(comment: Comment) {}
}