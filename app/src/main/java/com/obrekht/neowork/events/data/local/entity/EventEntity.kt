package com.obrekht.neowork.events.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.core.model.Coordinates
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.model.EventType
import com.obrekht.neowork.userpreview.model.UserPreview
import java.time.Instant

@Entity("event")
data class EventEntity(
    @PrimaryKey
    val eventId: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val datetime: Instant?,
    val published: Instant?,
    @Embedded
    val coords: Coordinates? = null,
    val type: EventType = EventType.OFFLINE,
    val likeOwnerIds: Set<Long> = emptySet(),
    val likedByMe: Boolean = false,
    val speakerIds: Set<Long> = emptySet(),
    val participantIds: Set<Long> = emptySet(),
    val participatedByMe: Boolean = false,
    @Embedded("attachment")
    val attachment: Attachment? = null,
    val link: String? = null
) {
    fun getUserIds() = likeOwnerIds.toMutableSet().apply {
        addAll(speakerIds)
        addAll(participantIds)
    }
}

fun EventEntity.toModel(users: Map<Long, UserPreview> = emptyMap()) = Event(
    eventId, authorId, author, authorJob, authorAvatar, content, datetime, published, coords, type,
    likeOwnerIds, likedByMe, speakerIds, participantIds, participatedByMe, attachment, link,
    users
)

fun Event.toEntity() = EventEntity(
    id, authorId, author, authorJob, authorAvatar, content, datetime, published, coords, type,
    likeOwnerIds, likedByMe, speakerIds, participantsIds, participatedByMe,
    attachment, link
)

fun List<EventEntity>.toModel() = map(EventEntity::toModel)
fun List<Event>.toEntity() = map { it.toEntity() }
