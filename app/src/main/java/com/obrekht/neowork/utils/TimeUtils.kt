package com.obrekht.neowork.utils

import android.content.Context
import android.text.format.DateUtils
import com.obrekht.neowork.R

object TimeUtils {
    fun getRelativeDate(context: Context, timestamp: Long): String {
        return if (System.currentTimeMillis() / 1000 - timestamp < 1) {
            context.getString(R.string.time_just_now)
        } else DateUtils.getRelativeDateTimeString(
            context,
            timestamp * DateUtils.SECOND_IN_MILLIS,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS * 2,
            0
        ).toString()
    }
}