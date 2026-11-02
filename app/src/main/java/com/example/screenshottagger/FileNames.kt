package com.example.screenshottagger

object FileNames {
    private const val MAX_LABEL_LENGTH = 40
    private val ILLEGAL = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val WHITESPACE = Regex("""\s+""")

    /** 去掉文件名非法字符、压缩空白、限制长度（不截断代理对）。 */
    fun sanitize(label: String): String {
        var clean = label.replace(ILLEGAL, "").replace(WHITESPACE, " ").trim()
        if (clean.length > MAX_LABEL_LENGTH) {
            clean = clean.substring(0, MAX_LABEL_LENGTH)
            if (clean.last().isHighSurrogate()) clean = clean.dropLast(1)
            clean = clean.trim()
        }
        return clean
    }

    /** `Screenshot_x.png` + `微信` → `Screenshot_x_微信.png`；已带标记或标签为空时返回 null。 */
    fun tagged(displayName: String, label: String): String? {
        val tag = sanitize(label)
        if (tag.isEmpty()) return null
        val (base, ext) = split(displayName)
        if (base.endsWith("_$tag")) return null
        return "${base}_$tag$ext"
    }

    /** `a.png`, 1 → `a (1).png` */
    fun withSuffix(name: String, n: Int): String {
        val (base, ext) = split(name)
        return "$base ($n)$ext"
    }

    private fun split(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) to name.substring(dot) else name to ""
    }
}
