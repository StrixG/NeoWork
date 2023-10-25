package com.obrekht.neowork.users.ui.profile.jobs

import com.obrekht.neowork.jobs.model.Job

interface JobInteractionListener {
    fun onClick(job: Job) {}
    fun onEdit(job: Job) {}
    fun onDelete(job: Job) {}
}