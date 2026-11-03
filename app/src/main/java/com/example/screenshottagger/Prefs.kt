package com.example.screenshottagger

import android.content.Context

object Prefs {
    private const val FILE = "settings"
    private const val KEY_ACTIVATED = "activated"

    /** 三项权限曾经全部开启过：之后再缺权限算“异常”，而不是“首次设置”。 */
    fun wasActivated(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVATED, false)

    fun setActivated(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVATED, true).apply()
}
