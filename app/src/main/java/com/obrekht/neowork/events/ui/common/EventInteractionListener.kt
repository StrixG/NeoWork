package com.obrekht.neowork.events.ui.common

import com.obrekht.neowork.events.model.Event

interface EventInteractionListener {
    fun onClick(event: Event) {}
    fun onAvatarClick(event: Event) {}
    fun onLike(event: Event) {}
    fun onShare(event: Event) {}
    fun onParticipate(event: Event) {}
    fun onEdit(event: Event) {}
    fun onDelete(event: Event) {}
}
