package com.obrekht.neowork.events.ui.common

import com.obrekht.neowork.events.model.Event
import java.time.LocalDate

sealed interface EventListItem

data class EventItem(val event: Event) : EventListItem
data class DateSeparatorItem(val date: LocalDate) : EventListItem
