package com.example.screenshottagger

import android.content.pm.PackageManager
import android.util.LruCache

/** 包名 → 应用显示名，小容量缓存；LruCache 本身线程安全。 */
class AppLabelResolver(private val pm: PackageManager) {
    private val cache = LruCache<String, String>(32)

    fun label(packageName: String): String {
        cache.get(packageName)?.let { return it }
        val label = try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
        cache.put(packageName, label)
        return label
    }

    fun clear() = cache.evictAll()
}
