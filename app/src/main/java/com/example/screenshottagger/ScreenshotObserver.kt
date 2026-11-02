package com.example.screenshottagger

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log

/**
 * 监听图库插入事件，发现新截图后按截图时刻查出前台应用并改名。
 * 只处理 NOTIFY_INSERT：自己改名产生的更新通知会被直接丢弃，不会循环。
 */
class ScreenshotObserver(
    private val context: Context,
    private val history: ForegroundHistory,
    private val labels: AppLabelResolver,
    main: Handler,
) : ContentObserver(main) {
    private val worker = IdleWorker(main)
    private val renamer = ScreenshotRenamer(context)
    private val handled = object : LinkedHashMap<Long, Unit>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Unit>) = size > 64
    }

    override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int) {
        // 个别 ROM 不带 flags（为 0），此时交给 handled 去重兜底
        if (flags != 0 && (flags and ContentResolver.NOTIFY_INSERT) == 0) return
        for (uri in uris) {
            val id = try {
                ContentUris.parseId(uri)
            } catch (e: NumberFormatException) {
                -1L
            }
            schedule(id, 0)
        }
    }

    fun shutdown() = worker.shutdown()

    /** 延迟处理：等系统写完文件、截图预览浮窗释放文件。 */
    private fun schedule(id: Long, attempt: Int) {
        worker.run(if (attempt == 0) FIRST_DELAY_MS else RETRY_DELAY_MS) { process(id, attempt) }
    }

    // 运行在工作线程
    private fun process(rawId: Long, attempt: Int) {
        if (!Prefs.isEnabled(context)) return
        // 通知里没带具体 id 时，退而查最近新增的一张
        val shot = (if (rawId > 0) renamer.query(rawId) else renamer.queryLatest())
        if (shot == null) {
            // 部分系统对他人尚未写完的 pending 项不可见，稍后再查
            if (rawId > 0 && attempt < MAX_RETRIES) schedule(rawId, attempt + 1)
            return
        }
        if (!shot.isScreenshot) return
        if (synchronized(handled) { handled.containsKey(shot.id) }) return
        if (shot.pending) {
            if (attempt < MAX_RETRIES) schedule(shot.id, attempt + 1)
            return
        }

        val pkg = history.appAt(shot.takenAt)
        if (pkg == null) {
            Log.i(TAG, "no foreground app for ${shot.displayName}")
            markHandled(shot.id)
            return
        }
        val newName = FileNames.tagged(shot.displayName, labels.label(pkg))
        if (newName == null) {
            markHandled(shot.id)
            return
        }
        val finalName = renamer.rename(shot, newName)
        if (finalName != null) {
            markHandled(shot.id)
            RenameLog.append(context, "${shot.displayName} → $finalName")
            Log.i(TAG, "renamed ${shot.displayName} -> $finalName")
        } else if (attempt < MAX_RETRIES) {
            schedule(shot.id, attempt + 1)
        } else {
            markHandled(shot.id)
            Log.w(TAG, "rename failed: ${shot.displayName}")
        }
    }

    private fun markHandled(id: Long) = synchronized(handled) { handled[id] = Unit }

    private companion object {
        const val TAG = "ScreenshotTagger"
        const val FIRST_DELAY_MS = 2_000L
        const val RETRY_DELAY_MS = 1_500L
        const val MAX_RETRIES = 3
    }
}
