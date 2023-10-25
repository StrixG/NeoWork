package com.obrekht.neowork.jobs.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.obrekht.neowork.jobs.model.Job
import java.time.Instant

@Entity("job", indices = [Index("userId")])
data class JobEntity(
    @PrimaryKey
    val jobId: Long,
    val userId: Long,
    val name: String,
    val position: String,
    val start: Instant,
    val finish: Instant? = null,
    val link: String? = null
)

fun JobEntity.toModel() = Job(jobId, name, position, start, finish, link)
fun Job.toEntity(userId: Long) = JobEntity(id, userId, name, position, start, finish, link)

fun List<JobEntity>.toModel() = map(JobEntity::toModel)
fun List<Job>.toEntity(userId: Long) = map { it.toEntity(userId) }
