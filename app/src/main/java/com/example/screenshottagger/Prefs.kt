package com.example.screenshottagger

import android.content.Context

object Prefs {
    private const val FILE = "settings"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).apply()
}
