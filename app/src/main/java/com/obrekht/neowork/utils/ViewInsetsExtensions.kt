package com.obrekht.neowork.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.setInsetsListener(listener: View.(insets: Insets) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val insets =
            windowInsets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars())

        listener(insets)

        windowInsets
    }
}