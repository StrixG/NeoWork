package com.obrekht.neowork.userlist.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.ItemUserBinding
import com.obrekht.neowork.users.model.User

typealias UserClickListener = (user: User, position: Int) -> Unit

class UserListAdapter(
    private val clickListener: UserClickListener
) : PagingDataAdapter<User, UserViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UserViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}

class UserViewHolder(
    private val binding: ItemUserBinding,
    private val clickListener: UserClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var user: User? = null

    init {
        with(binding) {
            card.setOnClickListener {
                user?.let { clickListener(it, bindingAdapterPosition) }
            }
        }
    }

    fun bind(user: User) {
        this.user = user

        with(binding) {
            user.avatar?.let { avatarUrl ->
                avatar.load(avatarUrl) {
                    placeholder(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                }
            } ?: avatar.setImageResource(R.drawable.avatar_placeholder)

            name.text = user.name
            login.text = user.login
        }
    }
}
