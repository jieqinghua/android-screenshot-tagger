package com.example.screenshottagger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机时检查一次：服务若被系统关掉则自动写回（需 WRITE_SECURE_SETTINGS），不常驻。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) A11yState.tryEnable(context)
    }
}
