package com.obrekht.neowork.posts.ui

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.obrekht.neowork.R
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.posts.ui.removeconfirmation.RemoveConfirmationDialogFragmentArgs
import com.obrekht.neowork.posts.ui.removeconfirmation.RemoveElementType

fun Fragment.sharePost(post: Post) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, post.content)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
    startActivity(shareIntent)
}

fun Fragment.showRemoveConfirmation(elementId: Long, elementType: RemoveElementType) {
    findNavController().navigate(
        R.id.remove_confirmation_dialog,
        RemoveConfirmationDialogFragmentArgs.Builder(elementId, elementType).build().toBundle()
    )
}