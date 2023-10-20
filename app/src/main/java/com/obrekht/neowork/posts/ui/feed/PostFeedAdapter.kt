package com.obrekht.neowork.posts.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
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

class PostFeedAdapter(
    private val interactionListener: PostInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
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
            is PostItem? -> (holder as? PostViewHolder)?.bind(item ?: PostItem(Post()))
            is DateSeparatorItem -> (holder as? DateSeparatorViewHolder)?.bind(item)
            else -> error("Unknown item type")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean = when {
            oldItem is PostItem && newItem is PostItem -> {
                oldItem.post.id == newItem.post.id
            }

            oldItem is DateSeparatorItem && newItem is DateSeparatorItem -> {
                oldItem.date == newItem.date
            }

            else -> false
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
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
            like.setOnClickListener {
                post?.let(interactionListener::onLike)
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

            like.setIconResource(
                if (post.likedByMe) {
                    R.drawable.ic_like
                } else {
                    R.drawable.ic_like_border
                }
            )
            like.text = StringUtils.getCompactNumber(post.likeOwnerIds.size)

            post.authorAvatar?.let {
                avatar.load(post.authorAvatar) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            post.attachment?.let {
                image.isVisible = it.type == AttachmentType.IMAGE

                when (it.type) {
                    AttachmentType.IMAGE -> {
                        image.load(it.url) {
                            scale(Scale.FILL)
                            crossfade(true)
                        }
                    }
                    AttachmentType.VIDEO -> {}
                    AttachmentType.AUDIO -> {}
                }
            } ?: run {
                image.isVisible = false
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