@file:UseSerializers(InstantSerializer::class)

package com.obrekht.neowork.jobs.model

import com.obrekht.neowork.core.data.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class Job(
    val id: Long = 0,
    val name: String = "",
    val position: String = "",
    val start: Instant = Instant.now(),
    val finish: Instant? = null,
    val link: String? = null
)
