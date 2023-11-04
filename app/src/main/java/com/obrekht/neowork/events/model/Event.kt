@file:UseSerializers(InstantSerializer::class)

package com.obrekht.neowork.events.model

import com.obrekht.neowork.core.data.serializer.InstantSerializer
import com.obrekht.neowork.core.model.Attachment
import com.obrekht.neowork.core.model.Coordinates
import com.obrekht.neowork.userpreview.model.UserPreview
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class Event(
    val id: Long = 0,
    val authorId: Long = 0,
    val author: String = "",
    val authorAvatar: String? = null,
    val content: String = "",
    val datetime: Instant? = null,
    val published: Instant? = null,
    val coords: Coordinates? = null,
    val type: EventType = EventType.OFFLINE,
    val likeOwnerIds: Set<Long> = emptySet(),
    val likedByMe: Boolean = false,
    val speakerIds: Set<Long> = emptySet(),
    val participantsIds: Set<Long> = emptySet(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val users: Map<Long, UserPreview> = emptyMap(),
    val ownedByMe: Boolean = false
)

enum class EventType {
    OFFLINE,
    ONLINE
}

