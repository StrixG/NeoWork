package com.obrekht.neowork.posts.ui

import android.content.Intent
import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.R
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteConfirmationDialogFragmentArgs
import com.obrekht.neowork.posts.ui.deleteconfirmation.DeleteElementType

fun Fragment.navigateToPost(postId: Long) {
    val action = NavGraphDirections.actionOpenPost(postId)
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

fun Fragment.showDeleteConfirmation(elementId: Long, elementType: DeleteElementType) {
    findRootNavController().navigate(
        R.id.delete_confirmation_dialog,
        DeleteConfirmationDialogFragmentArgs.Builder(elementId, elementType).build().toBundle()
    )
}