package kz.qorgau.scamguardian.ui.util

import android.content.Context
import kz.qorgau.scamguardian.R

fun formatTimeAgo(context: Context, epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val delta = (nowMs - epochMs).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    val hours = delta / 3_600_000L
    val days = delta / 86_400_000L
    return when {
        minutes < 1L -> context.getString(R.string.time_just_now)
        minutes < 60L -> context.getString(R.string.time_minutes_ago, minutes.toInt())
        hours < 24L -> context.getString(R.string.time_hours_ago, hours.toInt())
        else -> context.getString(R.string.time_days_ago, days.toInt().coerceAtLeast(1))
    }
}
