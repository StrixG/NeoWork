package com.obrekht.neowork.events.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.obrekht.neowork.databinding.ItemEventLoadStateBinding

class EventLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<EventLoadStateAdapter.ViewHolder>() {

    class ViewHolder(
        private val binding: ItemEventLoadStateBinding,
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
            ItemEventLoadStateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, retry)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        loadState: LoadState
    ) = holder.bind(loadState)
}
