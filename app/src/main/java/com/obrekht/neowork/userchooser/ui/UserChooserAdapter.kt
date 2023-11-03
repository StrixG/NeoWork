package com.obrekht.neowork.userchooser.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.ItemUserBinding
import com.obrekht.neowork.users.model.User

interface UserInteractionListener {
    fun onClick(user: User, position: Int) {}
    fun onCheckedChange(user: User, isChecked: Boolean, position: Int) {}
}

class UserChooserAdapter(
    private val interactionListener: UserInteractionListener
) : PagingDataAdapter<UserItem, UserViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UserViewHolder(binding, interactionListener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    class DiffCallback : DiffUtil.ItemCallback<UserItem>() {
        override fun areItemsTheSame(oldItem: UserItem, newItem: UserItem): Boolean =
            oldItem.user.id == newItem.user.id

        override fun areContentsTheSame(oldItem: UserItem, newItem: UserItem): Boolean =
            oldItem == newItem
    }
}

class UserViewHolder(
    private val binding: ItemUserBinding,
    private val interactionListener: UserInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    private var user: User? = null

    init {
        with(binding) {
            card.setOnClickListener {
                checkbox.toggle()
                user?.let {
                    interactionListener.onClick(it, bindingAdapterPosition)
                }
            }
            card.setOnLongClickListener {
                user?.let {
                    checkbox.toggle()
                    true
                } ?: false
            }
            checkbox.setOnCheckedChangeListener { _, state ->
                user?.let {
                    interactionListener.onCheckedChange(it, state, bindingAdapterPosition)
                }
            }
        }
    }

    fun bind(item: UserItem) {
        val user = item.user
        this.user = user

        with(binding) {
            user.avatar?.let { avatarUrl ->
                avatar.load(avatarUrl) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)

            name.text = user.name
            login.text = user.login
            checkbox.isVisible = true
            checkbox.isChecked = item.isSelected
        }
    }
}
