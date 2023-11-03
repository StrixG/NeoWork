package com.obrekht.neowork.utils

import android.content.res.Configuration

val Configuration.isLightTheme: Boolean
    get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
