package com.example.screenshottagger

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.util.Log
import java.io.File

class ScreenshotRenamer(private val context: Context) {

    class Shot(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val relativePath: String,
        val path: String?,
        val pending: Boolean,
        val takenAt: Long,
    ) {
        val isScreenshot get() = relativePath.contains("screenshot", ignoreCase = true)
    }

    /** 只查这一行、只取需要的列。 */
    fun query(id: Long): Shot? = query(
        Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${Media._ID}=?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(id.toString()))
        },
    )

    /** 最近 10 秒内新增的最新一张。 */
    fun queryLatest(): Shot? = query(
        Bundle().apply {
            val since = System.currentTimeMillis() / 1000 - 10
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${Media.DATE_ADDED}>=?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(since.toString()))
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${Media.DATE_ADDED} DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        },
    )

    private fun query(args: Bundle): Shot? {
        // 默认会排除 pending 项，这里显式包含，以便等待其写完
        args.putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
        val cursor = try {
            context.contentResolver.query(Media.EXTERNAL_CONTENT_URI, PROJECTION, args, null)
        } catch (e: SecurityException) {
            Log.w(TAG, "no storage permission", e)
            null
        } ?: return null
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            val id = c.getLong(0)
            val taken = c.getLong(5)
            return Shot(
                id = id,
                uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id),
                displayName = c.getString(1) ?: return null,
                relativePath = c.getString(2) ?: "",
                path = c.getString(3),
                pending = c.getInt(4) != 0,
                // DATE_ADDED 只有秒精度，取该秒末尾
                takenAt = if (taken > 0) taken else c.getLong(6) * 1000 + 999,
            )
        }
    }

    /** 返回最终文件名，失败返回 null。 */
    fun rename(shot: Shot, newName: String): String? {
        val dir = shot.path?.let { File(it).parentFile }
        var name = newName
        if (dir != null) {
            var n = 1
            while (File(dir, name).exists()) name = FileNames.withSuffix(newName, n++)
        }

        // 首选：通过 MediaStore 改名，图库数据库与文件同步更新
        try {
            val values = ContentValues().apply { put(Media.DISPLAY_NAME, name) }
            if (context.contentResolver.update(shot.uri, values, null, null) > 0) return name
        } catch (e: Exception) {
            Log.d(TAG, "MediaStore rename failed, falling back to File", e)
        }

        // 兜底：直接改文件，再让媒体库重新扫描新旧路径
        val oldFile = shot.path?.let(::File) ?: return null
        val newFile = File(oldFile.parentFile, name)
        if (!oldFile.renameTo(newFile)) return null
        MediaScannerConnection.scanFile(context, arrayOf(oldFile.path, newFile.path), null, null)
        return name
    }

    private companion object {
        const val TAG = "ScreenshotTagger"

        @Suppress("DEPRECATION")
        val PROJECTION = arrayOf(
            Media._ID,
            Media.DISPLAY_NAME,
            Media.RELATIVE_PATH,
            Media.DATA,
            Media.IS_PENDING,
            Media.DATE_TAKEN,
            Media.DATE_ADDED,
        )
    }
}
