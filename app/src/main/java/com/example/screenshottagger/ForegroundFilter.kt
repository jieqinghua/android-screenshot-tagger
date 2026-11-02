package com.example.screenshottagger

/** 不应被当作"截图对象"的窗口：系统界面、截图/长截图工具、输入法。本应用自身照常记录。 */
class ForegroundFilter(
    private val selfPackage: String,
    private val imePackages: Set<String>,
) {
    fun ignore(packageName: String): Boolean =
        packageName != selfPackage && (
            packageName == "com.android.systemui" ||
                packageName in imePackages ||
                SCREENSHOT_TOOL_KEYWORDS.any { packageName.contains(it, ignoreCase = true) }
            )

    private companion object {
        // 小米/OPPO/华为: *screenshot*；vivo 超级截屏: com.vivo.smartshot；三星: *smartcapture*
        val SCREENSHOT_TOOL_KEYWORDS = arrayOf("screenshot", "smartshot", "smartcapture")
    }
}
