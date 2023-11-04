package com.obrekht.neowork.map.ui

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.obrekht.neowork.databinding.ItemGeoSuggestBinding
import com.yandex.mapkit.search.SuggestItem

typealias GeoSuggestClickListener = (item: SuggestItem) -> Unit

class GeoSuggestAdapter(
    private val clickListener: GeoSuggestClickListener
) : ListAdapter<SuggestItem, GeoSuggestViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeoSuggestViewHolder {
        val binding = ItemGeoSuggestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return GeoSuggestViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: GeoSuggestViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    class DiffCallback : DiffUtil.ItemCallback<SuggestItem>() {
        override fun areItemsTheSame(oldItem: SuggestItem, newItem: SuggestItem): Boolean {
            return oldItem.title.text == newItem.title.text
        }

        override fun areContentsTheSame(oldItem: SuggestItem, newItem: SuggestItem): Boolean {
            return oldItem.subtitle?.text == newItem.subtitle?.text
                    && oldItem.distance?.text == newItem.distance?.text
        }
    }
}

class GeoSuggestViewHolder(
    private val binding: ItemGeoSuggestBinding,
    private val clickListener: GeoSuggestClickListener,
) : ViewHolder(binding.root) {

    private var suggestItem: SuggestItem? = null

    init {
        itemView.setOnClickListener {
            suggestItem?.let { clickListener(it) }
        }
    }

    fun bind(item: SuggestItem) = with(binding) {
        suggestItem = item

        val title = SpannableStringBuilder(item.title.text).apply {
            for (span in item.title.spans) {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    span.begin,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        name.text = title
        area.text = item.subtitle?.text
        distance.text = item.distance?.text
    }
}
