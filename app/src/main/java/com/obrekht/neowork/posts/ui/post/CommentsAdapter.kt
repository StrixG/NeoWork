package com.obrekht.neowork.posts.ui.post

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.ItemCommentBinding
import com.obrekht.neowork.posts.model.Comment
import com.obrekht.neowork.utils.StringUtils

class CommentsAdapter(
    private val interactionListener: CommentInteractionListener
) : ListAdapter<Comment, CommentsAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(
        private val binding: ItemCommentBinding,
        private val interactionListener: CommentInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var comment: Comment? = null

        init {
            with(binding) {
                root.setOnClickListener {
                    comment?.let(interactionListener::onClick)
                }
                like.setOnClickListener {
                    comment?.let(interactionListener::onLike)
                }
            }
        }

        fun bind(comment: Comment) {
            this.comment = comment

            with(binding) {
                val publishedMilli = comment.published?.toEpochMilli() ?: 0
                val publishedString = DateUtils.getRelativeDateTimeString(
                    published.context, publishedMilli,
                    DateUtils.SECOND_IN_MILLIS, DateUtils.DAY_IN_MILLIS * 2, 0
                )

                published.isVisible = publishedMilli > 0

                author.text = comment.author
                published.text = publishedString
                content.text = comment.content

                like.setIconResource(
                    if (comment.likedByMe) {
                        R.drawable.ic_like
                    } else {
                        R.drawable.ic_like_border
                    }
                )
                like.text = StringUtils.getCompactNumber(comment.likeOwnerIds.size)

                comment.authorAvatar?.let {
                    avatar.load(it) {
                        placeholder(R.drawable.avatar_placeholder)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding, interactionListener)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = getItem(position)
        holder.bind(comment)
    }
}
