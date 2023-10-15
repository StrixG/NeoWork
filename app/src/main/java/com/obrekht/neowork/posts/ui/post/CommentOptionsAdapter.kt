package com.obrekht.neowork.posts.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.neowork.databinding.ItemCommentOptionBinding

typealias CommentOptionClickListener = () -> Unit

class CommentOptionsAdapter(
    private val onItemClick: CommentOptionClickListener? = null
) : RecyclerView.Adapter<CommentOptionsAdapter.ViewHolder>() {

    private val _list: MutableList<CommentOption> = mutableListOf()
    val list: List<CommentOption> = _list

    class ViewHolder(
        private val binding: ItemCommentOptionBinding,
        private val onItemClick: CommentOptionClickListener?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(commentOption: CommentOption) {
            with(binding) {
                itemView.setOnClickListener {
                    onItemClick?.invoke()
                    commentOption.onClick()
                }
                optionIcon.setImageResource(commentOption.iconResId)
                optionLabel.text = optionLabel.resources.getString(commentOption.labelResId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCommentOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(_list[position])
    }

    override fun getItemCount(): Int = _list.size

    fun addItem(commentOption: CommentOption) = _list.add(commentOption)

    fun addItems(vararg commentOption: CommentOption) = _list.addAll(commentOption)
}

data class CommentOption(
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int,
    val onClick: CommentOptionClickListener
)
