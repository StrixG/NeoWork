package com.obrekht.neowork.posts.ui

import android.content.Intent
import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.R
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.editor.ui.editor.EditableType
import com.obrekht.neowork.posts.model.Post

fun Fragment.navigateToPost(postId: Long) {
    val action = NavGraphDirections.actionOpenPost(postId)
    findRootNavController().navigate(action)
}

fun Fragment.navigateToPostEditor(postId: Long = 0) {
    val action = NavGraphDirections.actionOpenEditor().apply {
        this.id = postId
        this.editableType = EditableType.POST
    }
    findRootNavController().navigate(action)
}

fun Fragment.navigateToCommentEditor(commentId: Long) {
    val action = NavGraphDirections.actionOpenEditor().apply {
        this.id = commentId
        this.editableType = EditableType.COMMENT
    }
    findRootNavController().navigate(action)
}

fun Fragment.sharePost(post: Post) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, post.content)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
    startActivity(shareIntent)
}
