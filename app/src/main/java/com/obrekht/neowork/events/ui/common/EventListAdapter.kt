package com.obrekht.neowork.events.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
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
import com.obrekht.neowork.media.util.retrieveMediaMetadata
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EventListAdapter(
    private val interactionListener: EventInteractionListener
) : PagingDataAdapter<EventListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val dateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())

    override fun getItemViewType(position: Int): Int = when (peek(position)) {
        is EventItem, null -> R.layout.item_event
        is DateSeparatorItem -> R.layout.item_date_separator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_event -> {
                val binding =
                    ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EventViewHolder(binding, interactionListener, scope, dateTimeFormatter)
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
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        when (val item = getItem(position)) {
            is EventItem? -> {
                val eventItem = item ?: EventItem()
                val eventHolder = holder as EventViewHolder

                if (payloads.isEmpty()) {
                    eventHolder.bind(eventItem)
                } else {
                    val payloadList = payloads.map { it as EventPayload }
                    eventHolder.bind(eventItem, payloadList)
                }
            }

            is DateSeparatorItem -> (holder as DateSeparatorViewHolder).bind(item)
            else -> error("Unknown item type")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EventListItem>() {
        override fun areItemsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean =
            when {
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

        override fun getChangePayload(
            oldItem: EventListItem,
            newItem: EventListItem
        ): EventPayload? {
            if (oldItem !is EventItem || newItem !is EventItem) return null

            val payload = EventPayload(
                likedByMe = newItem.event.likedByMe.takeIf {
                    oldItem.event.likedByMe != it
                },
                participatedByMe = newItem.event.participatedByMe.takeIf {
                    oldItem.event.participatedByMe != it
                },
                isPlaying = newItem.isAudioPlaying.takeIf {
                    oldItem.isAudioPlaying != it
                }
            )
            return payload.takeIf { it != EventPayload.EMPTY }
        }
    }

    data class EventPayload(
        val likedByMe: Boolean? = null,
        val participatedByMe: Boolean? = null,
        val isPlaying: Boolean? = null
    ) {
        companion object {
            val EMPTY = EventPayload()
        }
    }
}

class EventViewHolder(
    private val binding: ItemEventBinding,
    private val interactionListener: EventInteractionListener,
    private val scope: CoroutineScope,
    private val dateTimeFormatter: DateTimeFormatter
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
            buttonPlayAudio.setOnClickListener {
                event?.let(interactionListener::onPlayAudioButtonClick)
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
            type.setText(
                when (event.type) {
                    EventType.OFFLINE -> R.string.event_type_offline
                    EventType.ONLINE -> R.string.event_type_online
                }
            )
            val dateMillis = event.datetime?.toEpochMilli() ?: 0
            date.text = dateTimeFormatter.format(Instant.ofEpochMilli(dateMillis))
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
            TooltipCompat.setTooltipText(
                participate, participate.resources.getString(
                    if (event.participatedByMe) {
                        R.string.do_not_participate
                    } else {
                        R.string.participate
                    }
                )
            )
            participate.isChecked = event.participatedByMe
            participate.text = StringUtils.getCompactNumber(event.participantsIds.size)

            // Attachment
            attachmentPreview.load(null)
            attachmentPreview.isVisible = false
            buttonPlayVideo.isVisible = false
            audioGroup.isVisible = false

            event.attachment?.let {
                when (it.type) {
                    AttachmentType.IMAGE -> loadImage(it.url)
                    AttachmentType.VIDEO -> loadVideo(it.url)
                    AttachmentType.AUDIO -> loadAudio(it.url, item.isAudioPlaying)
                }
            }
        }
    }

    fun bind(
        item: EventItem,
        payloadList: List<EventListAdapter.EventPayload>
    ) {
        val event = item.event
        this.event = event

        with(binding) {
            payloadList.forEach { payload ->
                payload.likedByMe?.let { likedByMe ->
                    like.isChecked = likedByMe
                    like.text = StringUtils.getCompactNumber(event.likeOwnerIds.size)
                }
                payload.participatedByMe?.let { participatedByMe ->
                    TooltipCompat.setTooltipText(
                        participate, participate.resources.getString(
                            if (participatedByMe) {
                                R.string.do_not_participate
                            } else {
                                R.string.participate
                            }
                        )
                    )
                    participate.isChecked = participatedByMe
                    participate.text = StringUtils.getCompactNumber(event.participantsIds.size)
                }
                payload.isPlaying?.let {
                    updatePlayAudioButton(it)
                }
            }
        }
    }

    private fun loadImage(url: String) = with(binding) {
        attachmentPreview.load(url) {
            crossfade(true)
        }
        attachmentPreview.isVisible = true
        attachmentPreview.transitionName = "${url}_$absoluteAdapterPosition"
    }

    private fun loadVideo(url: String) = with(binding) {
        attachmentPreview.load(url) {
            crossfade(true)
            listener { _, _ ->
                buttonPlayVideo.isVisible = true
            }
        }
        attachmentPreview.isVisible = true
        attachmentPreview.transitionName = "${url}_$absoluteAdapterPosition"
    }

    private fun loadAudio(url: String, isAudioPlaying: Boolean) = with(binding) {
        audioGroup.isVisible = true
        audioTitle.setText(R.string.loading)
        audioArtist.text = null

        updatePlayAudioButton(isAudioPlaying)

        scope.launch {
            val context = audioGroup.context
            val mediaItem = MediaItem.fromUri(url)

            val mediaMetadata = mediaItem.retrieveMediaMetadata(context)
            mediaMetadata?.let {
                audioTitle.text = mediaMetadata.title
                    ?: context.getString(R.string.audio_untitled)
                audioArtist.text = mediaMetadata.artist
                    ?: context.getString(R.string.audio_unknown_artist)
            } ?: run {
                audioTitle.text = context.getString(R.string.audio_unknown)
            }
        }
    }

    private fun updatePlayAudioButton(isPlaying: Boolean) {
        val playIcon = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }
        binding.buttonPlayAudio.setIconResource(playIcon)
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
