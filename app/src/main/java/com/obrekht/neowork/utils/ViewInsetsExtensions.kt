package com.obrekht.neowork.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.setBarsInsetsListener(insetIme: Boolean = true, listener: View.(insets: Insets) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        var mask = WindowInsetsCompat.Type.systemBars()
        if (insetIme) {
            mask = mask or WindowInsetsCompat.Type.ime()
        }
        val insets = windowInsets.getInsets(mask)

        listener(insets)

        windowInsets
    }
}
