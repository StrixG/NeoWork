package com.obrekht.neowork.events.ui

import android.content.Intent
import androidx.fragment.app.Fragment
import com.obrekht.neowork.R
import com.obrekht.neowork.events.model.Event

fun Fragment.navigateToEvent(eventId: Long) {
    // TODO: Open screen with event details
//    val action = NavGraphDirections.actionOpenEvent(eventId)
//    findRootNavController().navigate(action)
}

fun Fragment.navigateToEventEditor(eventId: Long = 0) {
    // TODO: Open event editor
//    val action = NavGraphDirections.actionOpenEditor().apply {
//        this.id = eventId
//        this.editableType = EditableType.EVENT
//    }
//    findRootNavController().navigate(action)
}

fun Fragment.shareEvent(event: Event) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, event.content)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_event))
    startActivity(shareIntent)
}
