package com.obrekht.neowork.posts.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.ItemDateSeparatorBinding
import com.obrekht.neowork.databinding.ItemPostBinding
import com.obrekht.neowork.posts.model.Post
import com.obrekht.neowork.utils.StringUtils
import com.obrekht.neowork.utils.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PostListAdapter(
    private val interactionListener: PostInteractionListener
) : PagingDataAdapter<PostListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int = when (peek(position)) {
        is PostItem, null -> R.layout.item_post
        is DateSeparatorItem -> R.layout.item_date_separator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> {
                val binding =
                    ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, interactionListener)
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
            is PostItem? -> (holder as PostViewHolder).bind(item ?: PostItem(Post()))
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
    }
}

class PostViewHolder(
    private val binding: ItemPostBinding,
    private val interactionListener: PostInteractionListener
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

            post.attachment?.let {
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
