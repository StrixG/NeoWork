package com.obrekht.neowork.users.ui.profile.jobs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.ItemJobBinding
import com.obrekht.neowork.jobs.model.Job
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class JobListAdapter(
    private val interactionListener: JobInteractionListener,
    var isOwnProfile: Boolean
) : PagingDataAdapter<Job, JobViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding, interactionListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, isOwnProfile) }
    }

    class DiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean =
            oldItem == newItem
    }
}

class JobViewHolder(
    private val binding: ItemJobBinding,
    private val interactionListener: JobInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    private var job: Job? = null
    private val popupMenu = PopupMenu(binding.menu.context, binding.menu).apply {
        inflate(R.menu.menu_job)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    job?.let(interactionListener::onEdit)
                    true
                }

                R.id.delete -> {
                    job?.let(interactionListener::onDelete)
                    true
                }

                else -> false
            }
        }
    }

    init {
        with(binding) {
            card.setOnClickListener {
                job?.let(interactionListener::onClick)
            }

            menu.setOnClickListener {
                popupMenu.show()
            }
        }
    }

    fun bind(job: Job, isOwnProfile: Boolean) {
        this.job = job

        val resources = itemView.resources

        with(binding) {
            menu.isVisible = isOwnProfile

            val jobStart = dateFormatter.format(job.start)
            val jobFinish = job.finish?.let {
                dateFormatter.format(it)
            } ?: resources.getString(R.string.present)

            val experienceString = "$jobStart – $jobFinish"

            name.text = job.name
            experience.text = experienceString
            position.text = job.position

            link.isVisible = job.link != null
            job.link?.let {
                link.text = it
            }
        }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withZone(ZoneId.systemDefault())
    }
}