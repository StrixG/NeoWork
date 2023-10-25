package com.obrekht.neowork.posts.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.neowork.databinding.ItemPostLoadStateBinding

class PostLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<PostLoadStateAdapter.ViewHolder>() {

    class ViewHolder(
        private val binding: ItemPostLoadStateBinding,
        retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.retryButton.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) = with(binding) {
            progress.isVisible = loadState is LoadState.Loading
            groupError.isVisible = loadState is LoadState.Error
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): ViewHolder {
        val binding =
            ItemPostLoadStateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, retry)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        loadState: LoadState
    ) = holder.bind(loadState)
}