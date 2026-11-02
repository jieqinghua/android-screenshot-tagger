package com.example.screenshottagger

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * 常驻核心：记录前台应用，并托管截图监听。
 * 无障碍服务由系统绑定，进程优先级高，因此无需前台服务/唤醒锁/定时器。
 */
class ForegroundAppService : AccessibilityService() {
    private val history = ForegroundHistory()
    private var filter: ForegroundFilter? = null
    private var observer: ScreenshotObserver? = null
    private var labels: AppLabelResolver? = null
    private var lastPackage: CharSequence? = null

    override fun onServiceConnected() {
        filter = ForegroundFilter(packageName, imePackages())
        val resolver = AppLabelResolver(packageManager).also { labels = it }
        observer = ScreenshotObserver(this, history, resolver, Handler(Looper.getMainLooper())).also {
            contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, it)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName ?: return
        // 热路径：同一应用内的窗口切换直接返回，不分配对象
        if (TextUtils.equals(pkg, lastPackage)) return
        // 其他应用弹出的 Toast 不代表前台切换
        if (event.className?.contains("Toast") == true) return
        val name = pkg.toString()
        lastPackage = name
        if (filter?.ignore(name) == true) return
        history.record(System.currentTimeMillis(), name)
    }

    override fun onInterrupt() = Unit

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        labels?.clear()
    }

    override fun onDestroy() {
        observer?.let {
            contentResolver.unregisterContentObserver(it)
            it.shutdown()
        }
        observer = null
        super.onDestroy()
    }

    private fun imePackages(): Set<String> {
        val imm = getSystemService(InputMethodManager::class.java) ?: return emptySet()
        return imm.enabledInputMethodList.mapTo(HashSet()) { it.packageName }
    }
}
