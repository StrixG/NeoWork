package com.obrekht.neowork.posts.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
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
import com.obrekht.neowork.databinding.ItemPostBinding
import com.obrekht.neowork.media.util.retrieveMediaMetadata
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PostListAdapter(
    private val interactionListener: PostInteractionListener
) : PagingDataAdapter<PostListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun getItemViewType(position: Int): Int = when (peek(position)) {
        is PostItem, null -> R.layout.item_post
        is DateSeparatorItem -> R.layout.item_date_separator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> {
                val binding =
                    ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, interactionListener, scope)
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
            is PostItem? -> {
                val postItem = item ?: PostItem()
                val postHolder = holder as PostViewHolder

                if (payloads.isEmpty()) {
                    postHolder.bind(postItem)
                } else {
                    val payloadList = payloads.map { it as PostPayload }
                    postHolder.bind(postItem, payloadList)
                }
            }

            is DateSeparatorItem -> (holder as DateSeparatorViewHolder).bind(item)
            else -> error("Unknown item type")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PostListItem>() {
        override fun areItemsTheSame(oldItem: PostListItem, newItem: PostListItem): Boolean = when {
            oldItem is PostItem && newItem is PostItem -> {
                oldItem.post.id == newItem.post.id
            }

            oldItem is DateSeparatorItem && newItem is DateSeparatorItem -> {
                oldItem.date == newItem.date
            }

            else -> false
        }

        override fun areContentsTheSame(oldItem: PostListItem, newItem: PostListItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: PostListItem, newItem: PostListItem): PostPayload? {
            if (oldItem !is PostItem || newItem !is PostItem) return null

            val payload = PostPayload(
                likedByMe = newItem.post.likedByMe.takeIf {
                    oldItem.post.likedByMe != it
                },
                isPlaying = newItem.isAudioPlaying.takeIf {
                    oldItem.isAudioPlaying != it
                }
            )
            return payload.takeIf { it != PostPayload.EMPTY }
        }
    }

    data class PostPayload(
        val likedByMe: Boolean? = null,
        val isPlaying: Boolean? = null
    ) {
        companion object {
            val EMPTY = PostPayload()
        }
    }
}

class PostViewHolder(
    private val binding: ItemPostBinding,
    private val interactionListener: PostInteractionListener,
    private val scope: CoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

    private var post: Post? = null
    private val popupMenu = PopupMenu(binding.menu.context, binding.menu).apply {
        inflate(R.menu.menu_post)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    post?.let(interactionListener::onEdit)
                    true
                }

                R.id.delete -> {
                    post?.let(interactionListener::onDelete)
                    true
                }

                else -> false
            }
        }
    }

    init {
        with(binding) {
            card.setOnClickListener {
                post?.let(interactionListener::onClick)
            }
            avatar.setOnClickListener {
                post?.let(interactionListener::onAvatarClick)
            }
            attachmentPreview.setOnClickListener {
                post?.let { interactionListener.onAttachmentClick(it, attachmentPreview) }
            }
            buttonPlayAudio.setOnClickListener {
                post?.let(interactionListener::onPlayAudioButtonClick)
            }
            like.setOnClickListener {
                var success = false
                post?.let {
                    success = interactionListener.onLike(it)
                }
                if (!success) {
                    like.toggle()
                }
            }
            share.setOnClickListener {
                post?.let(interactionListener::onShare)
            }

            menu.setOnClickListener {
                popupMenu.show()
            }
        }
    }

    fun bind(item: PostItem) {
        val post = item.post
        this.post = post

        with(binding) {
            menu.isVisible = post.ownedByMe

            author.text = post.author
            content.text = post.content
            refreshPublishedDate()

            // Avatar
            post.authorAvatar?.let {
                avatar.load(post.authorAvatar) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            // Like
            like.isChecked = post.likedByMe
            like.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)

            // Attachment
            attachmentPreview.transitionName = null
            attachmentPreview.load(null)
            attachmentPreview.isVisible = false
            buttonPlayVideo.isVisible = false
            audioGroup.isVisible = false

            post.attachment?.let {
                when (it.type) {
                    AttachmentType.IMAGE -> loadImage(it.url)
                    AttachmentType.VIDEO -> loadVideo(it.url)
                    AttachmentType.AUDIO -> loadAudio(it.url, item.isAudioPlaying)
                }
            }
        }
    }

    fun bind(
        item: PostItem,
        payloadList: List<PostListAdapter.PostPayload>
    ) {
        val post = item.post
        this.post = post

        with(binding) {
            payloadList.forEach { payload ->
                payload.likedByMe?.let { likedByMe ->
                    like.isChecked = likedByMe
                    like.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)
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

    fun refreshPublishedDate() = post?.let {
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
