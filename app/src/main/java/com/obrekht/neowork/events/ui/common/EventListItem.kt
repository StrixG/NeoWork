package com.obrekht.neowork.events.ui.common

import com.obrekht.neowork.events.model.Event
import java.time.LocalDate

sealed interface EventListItem

data class EventItem(
    val event: Event = Event(),
    val isAudioPlaying: Boolean = false
) : EventListItem
data class DateSeparatorItem(val date: LocalDate) : EventListItem
