package com.example.screenshottagger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** 读写系统"已启用的无障碍服务"列表。 */
object A11yState {
    private fun component(context: Context) =
        ComponentName(context, ForegroundAppService::class.java).flattenToString()

    private fun enabledServices(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""

    fun isEnabled(context: Context): Boolean {
        val self = component(context)
        return enabledServices(context).split(':').any { it.equals(self, ignoreCase = true) }
    }

    /** 需要 adb 授予 WRITE_SECURE_SETTINGS；没有该权限时返回 false。 */
    fun tryEnable(context: Context): Boolean {
        if (isEnabled(context)) return true
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val current = enabledServices(context)
        val updated = if (current.isEmpty()) component(context) else "$current:${component(context)}"
        val resolver = context.contentResolver
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        return true
    }
}
