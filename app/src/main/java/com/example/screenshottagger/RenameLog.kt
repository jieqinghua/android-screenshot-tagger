package com.example.screenshottagger

import android.content.Context
import java.io.File

/** 最近改名记录：只在改名时写一个小文件，打开设置页时才读，不常驻内存。 */
object RenameLog {
    private const val FILE = "rename_log.txt"
    private const val MAX_LINES = 20

    @Synchronized
    fun append(context: Context, line: String) {
        val file = File(context.filesDir, FILE)
        val lines = read(file) + line
        file.writeText(lines.takeLast(MAX_LINES).joinToString("\n"))
    }

    /** 最新的在前。 */
    @Synchronized
    fun read(context: Context): List<String> = read(File(context.filesDir, FILE)).asReversed()

    private fun read(file: File): List<String> =
        if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()
}
