package com.obrekht.neowork.events.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.ItemDateSeparatorBinding
import com.obrekht.neowork.databinding.ItemEventBinding
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.model.EventType
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EventListAdapter(
    private val interactionListener: EventInteractionListener
) : PagingDataAdapter<EventListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int = when (peek(position)) {
        is EventItem, null -> R.layout.item_event
        is DateSeparatorItem -> R.layout.item_date_separator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_event -> {
                val binding =
                    ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EventViewHolder(binding, interactionListener)
            }

            R.layout.item_date_separator -> {
                val binding = ItemDateSeparatorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateSeparatorViewHolder(binding)
            }

            else -> error("Unknown item view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EventItem? -> (holder as EventViewHolder).bind(item ?: EventItem(Event()))
            is DateSeparatorItem -> (holder as DateSeparatorViewHolder).bind(item)
            else -> error("Unknown item type")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EventListItem>() {
        override fun areItemsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean = when {
            oldItem is EventItem && newItem is EventItem -> {
                oldItem.event.id == newItem.event.id
            }

            oldItem is DateSeparatorItem && newItem is DateSeparatorItem -> {
                oldItem.date == newItem.date
            }

            else -> false
        }

        override fun areContentsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean {
            return oldItem == newItem
        }
    }
}

class EventViewHolder(
    private val binding: ItemEventBinding,
    private val interactionListener: EventInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    private var event: Event? = null
    private val popupMenu = PopupMenu(binding.menu.context, binding.menu).apply {
        inflate(R.menu.menu_event)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    event?.let(interactionListener::onEdit)
                    true
                }

                R.id.delete -> {
                    event?.let(interactionListener::onDelete)
                    true
                }

                else -> false
            }
        }
    }

    init {
        with(binding) {
            card.setOnClickListener {
                event?.let(interactionListener::onClick)
            }
            avatar.setOnClickListener {
                event?.let(interactionListener::onAvatarClick)
            }
            like.setOnClickListener {
                var success = false
                event?.let {
                    success = interactionListener.onLike(it)
                }
                if (!success) {
                    like.toggle()
                }
            }
            share.setOnClickListener {
                event?.let(interactionListener::onShare)
            }
            attachmentPreview.setOnClickListener {
                event?.let { interactionListener.onAttachmentClick(it, attachmentPreview) }
            }
            participate.setOnClickListener {
                var success = false
                event?.let {
                    success = interactionListener.onParticipate(it)
                }
                if (!success) {
                    like.toggle()
                }
            }

            menu.setOnClickListener {
                popupMenu.show()
            }
        }
    }

    fun bind(item: EventItem) {
        val event = item.event
        this.event = event

        with(binding) {
            menu.isVisible = event.ownedByMe

            refreshPublishedDate()
            author.text = event.author
            type.setText(when(event.type) {
                EventType.OFFLINE -> R.string.event_type_offline
                EventType.ONLINE -> R.string.event_type_online
            })
            val dateMillis = event.datetime?.toEpochMilli() ?: 0
            date.text = TimeUtils.getRelativeDate(
                itemView.context,
                dateMillis
            )
            content.text = event.content

            // Avatar
            event.authorAvatar?.let {
                avatar.load(event.authorAvatar) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            // Like
            like.isChecked = event.likedByMe
            like.text = StringUtils.getCompactNumber(event.likeOwnerIds.size)

            // Participation
            TooltipCompat.setTooltipText(participate, participate.resources.getString(
                if (event.participatedByMe) {
                    R.string.do_not_participate
                } else {
                    R.string.participate
                }
            ))
            participate.isChecked = event.participatedByMe
            participate.text = StringUtils.getCompactNumber(event.participantsIds.size)

            // Attachment
            attachmentPreview.load(null)
            attachmentPreview.isVisible = false
            buttonPlayVideo.isVisible = false

            event.attachment?.let {
                when (it.type) {
                    AttachmentType.IMAGE -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                        }
                        attachmentPreview.isVisible = true
                        attachmentPreview.transitionName = "${it.url}_$absoluteAdapterPosition"
                    }
                    AttachmentType.VIDEO -> {
                        attachmentPreview.load(it.url) {
                            crossfade(true)
                            listener { _, _ ->
                                buttonPlayVideo.isVisible = true
                            }
                        }
                        attachmentPreview.isVisible = true
                        attachmentPreview.transitionName = "${it.url}_$absoluteAdapterPosition"
                    }
                    AttachmentType.AUDIO -> {}
                }
            }
        }
    }

    fun refreshPublishedDate() = event?.let {
        val publishedMillis = it.published?.toEpochMilli() ?: 0

        binding.published.text = TimeUtils.getRelativeDate(
            itemView.context,
            publishedMillis
        )
    }
}

class DateSeparatorViewHolder(
    private val binding: ItemDateSeparatorBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: DateSeparatorItem) {
        val resources = binding.root.resources
        val now = LocalDate.now()
        val yesterday = now.minusDays(1)

        binding.dateText.text = when (item.date) {
            now -> resources.getString(R.string.today)
            yesterday -> resources.getString(R.string.yesterday)
            else -> item.date.format(formatter)
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
