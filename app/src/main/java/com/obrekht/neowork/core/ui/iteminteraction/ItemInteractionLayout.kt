package com.obrekht.neowork.core.ui.iteminteraction

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.ViewItemInteractionBinding
import com.obrekht.neowork.userpreview.model.UserPreview

class ItemInteractionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewItemInteractionBinding.inflate(LayoutInflater.from(context), this, true)

    val button = binding.button
    val preview = binding.preview

    init {
        context.withStyledAttributes(attrs, R.styleable.ItemInteractionLayout) {
            binding.button.isCheckable = getBoolean(R.styleable.ItemInteractionLayout_buttonCheckable, true)
            binding.button.isVisible = getBoolean(R.styleable.ItemInteractionLayout_showButton, true)
            binding.title.text = getString(R.styleable.ItemInteractionLayout_title)
            val iconId = getResourceId(R.styleable.ItemInteractionLayout_buttonIcon, 0)
            if (iconId != 0) {
                button.icon = AppCompatResources.getDrawable(context, iconId)
            }
        }
    }

    fun setButtonClickListener(listener: OnClickListener) {
        button.setOnClickListener(listener)
    }

    fun setUserPreviews(previewMap: Map<Long, UserPreview>) = preview.setPreviews(previewMap)
}
