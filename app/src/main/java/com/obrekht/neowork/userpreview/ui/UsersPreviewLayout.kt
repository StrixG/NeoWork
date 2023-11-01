package com.obrekht.neowork.userpreview.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.obrekht.neowork.R
import com.obrekht.neowork.userpreview.model.UserPreview
import kotlin.math.ceil

private const val MAX_USER_COUNT = 5

typealias UserPreviewClickListener = (userId: Long) -> Unit
typealias UserPreviewMoreClickListener = () -> Unit

class UsersPreviewLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val horizontalMargin =
        context.resources.getDimension(R.dimen.users_preview_margin).toInt()

    private val previewSize =
        context.resources.getDimension(R.dimen.user_preview_size).toInt()

    private val previewStrokeWidth =
        context.resources.getDimension(R.dimen.user_preview_stroke_width)

    private val previewStrokeColor = TypedValue().run {
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, this, true)
        AppCompatResources.getColorStateList(context, resourceId)
    }

    private var previewViews: List<ShapeableImageView> = emptyList()
    private val moreView: View

    private var previewClickListener: UserPreviewClickListener? = null
    private var moreClickListener: UserPreviewMoreClickListener? = null

    init {
        with(mutableListOf<ShapeableImageView>()) {
            repeat(MAX_USER_COUNT) {
                val view = createUserPreviewView().apply {
                    isVisible = false
                    add(this)
                }
                addViewInLayout(view, -1, view.layoutParams)
            }
            previewViews = this
        }

        moreView = createUserPreviewView().apply {
            setImageResource(R.drawable.ic_user_preview_more)
            setOnClickListener {
                moreClickListener?.invoke()
            }
            isVisible = false
        }
        applyRipple(moreView)
        addViewInLayout(moreView, -1, moreView.layoutParams)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var left = paddingLeft
        var maxLineHeight = 0

        previewViews.forEach {
            if (!it.isGone) {
                measureChild(it, widthMeasureSpec, heightMeasureSpec)
                left += it.measuredWidth + it.marginRight + horizontalMargin
                maxLineHeight = maxOf(maxLineHeight, it.measuredHeight)
            }
        }

        if (!moreView.isGone) {
            measureChild(moreView, widthMeasureSpec, heightMeasureSpec)
            left += moreView.measuredWidth + moreView.marginRight + horizontalMargin
            maxLineHeight = maxOf(maxLineHeight, moreView.measuredHeight)
        }

        setMeasuredDimension(
            resolveSize(left - horizontalMargin + paddingRight, widthMeasureSpec),
            resolveSize(maxLineHeight + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = paddingLeft
        val top = paddingTop

        previewViews.forEach {
            if (!it.isGone) {
                it.layout(
                    left + it.marginLeft,
                    top + it.marginTop,
                    left + it.measuredWidth,
                    top + it.measuredHeight
                )

                left += it.measuredWidth + it.marginRight + horizontalMargin
            }
        }

        if (!moreView.isGone) {
            moreView.layout(
                left + moreView.marginLeft,
                top + moreView.marginTop,
                left + moreView.measuredWidth,
                top + moreView.measuredHeight
            )
        }
    }

    fun setPreviews(previewMap: Map<Long, UserPreview>) {
        previewViews.forEach { it.isVisible = false }

        previewMap.entries.take(MAX_USER_COUNT).forEachIndexed { index, (userId, preview) ->
            previewViews[index].apply {
                load(preview.avatar) {
                    placeholder(R.drawable.avatar_placeholder)
                    error(R.drawable.avatar_placeholder)
                    transformations(CircleCropTransformation())
                    crossfade(true)
                }

                TooltipCompat.setTooltipText(this, preview.name)

                setOnClickListener {
                    previewClickListener?.invoke(userId)
                }
                isVisible = true
            }
        }
        moreView.isVisible = previewMap.size > MAX_USER_COUNT
        requestLayout()
    }

    private fun applyRipple(view: View) = with(view) {
        with(TypedValue()) {
            context.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionBarItemBackground,
                this,
                true
            )
            foreground = ContextCompat.getDrawable(context, resourceId)
        }
    }

    fun setOnPreviewClickListener(listener: UserPreviewClickListener) {
        previewClickListener = listener
    }

    fun setOnMoreClickListener(listener: UserPreviewMoreClickListener) {
        moreClickListener = listener
    }

    private fun createUserPreviewView(): ShapeableImageView {
        return ShapeableImageView(context).apply {
            layoutParams = MarginLayoutParams(previewSize, previewSize)
            setPadding(ceil(previewStrokeWidth / 2).toInt())
            shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL)
                .build()
            strokeColor = previewStrokeColor
            strokeWidth = previewStrokeWidth
            applyRipple(this)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams = MarginLayoutParams(p)
}
