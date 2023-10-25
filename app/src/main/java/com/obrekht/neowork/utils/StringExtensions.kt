package com.obrekht.neowork.utils

import android.util.Patterns

fun String.isValidWebUrl() =
    Patterns.WEB_URL.matcher(this).matches()