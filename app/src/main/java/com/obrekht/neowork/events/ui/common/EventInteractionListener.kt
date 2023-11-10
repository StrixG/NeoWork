package com.obrekht.neowork.events.ui.common

import android.widget.ImageView
import com.obrekht.neowork.events.model.Event

interface EventInteractionListener {
    fun onClick(event: Event) {}
    fun onAvatarClick(event: Event) {}
    fun onAttachmentClick(event: Event, view: ImageView) {}
    fun onLike(event: Event) {}
    fun onShare(event: Event) {}
    fun onParticipate(event: Event) {}
    fun onEdit(event: Event) {}
    fun onDelete(event: Event) {}
}
