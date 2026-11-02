package com.example.screenshottagger

/** 不应被当作"截图对象"的窗口：系统界面、截图浮窗、输入法、本应用。 */
class ForegroundFilter(
    private val selfPackage: String,
    private val imePackages: Set<String>,
) {
    fun ignore(packageName: String): Boolean =
        packageName == selfPackage ||
            packageName == "com.android.systemui" ||
            packageName in imePackages ||
            packageName.contains("screenshot", ignoreCase = true)
}
