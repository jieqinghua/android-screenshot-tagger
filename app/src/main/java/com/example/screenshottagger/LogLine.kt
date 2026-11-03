package com.example.screenshottagger

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** 改名记录的一行，拆成首页要显示的部分：新文件名、新增标记的位置、截图时间。 */
class LogLine(val newName: String, val tagStart: Int, val tagEnd: Int, val time: LocalDateTime?) {
    val hasTag get() = tagStart in 0 until tagEnd

    companion object {
        /** RenameLog 写入的格式：`原名 → 新名 (包名)`。 */
        private val LINE = Regex("""^(.*) → (.*) \([^()]*\)$""")
        private val DUP_SUFFIX = Regex(""" \(\d+\)$""")
        private val TIME = Regex("""(\d{8})[_-]?(\d{6})""")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        /** 认不出的行原样返回，不高亮、无时间。 */
        fun parse(line: String): LogLine {
            val match = LINE.matchEntire(line) ?: return LogLine(line, -1, -1, null)
            val (old, new) = match.destructured
            val oldBase = base(old)
            val newBase = base(new)
            var start = -1
            var end = -1
            if (newBase.length > oldBase.length && newBase.startsWith(oldBase)) {
                start = oldBase.length
                // 重名时追加的 " (1)" 不算标记
                end = DUP_SUFFIX.find(newBase)?.range?.first?.takeIf { it > start } ?: newBase.length
            }
            return LogLine(new, start, end, time(new))
        }

        private fun base(name: String): String {
            val dot = name.lastIndexOf('.')
            return if (dot > 0) name.substring(0, dot) else name
        }

        private fun time(name: String): LocalDateTime? {
            val m = TIME.find(name) ?: return null
            return try {
                LocalDateTime.parse(m.groupValues[1] + m.groupValues[2], TIME_FORMAT)
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }
}
