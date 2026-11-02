package com.example.screenshottagger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundFilterTest {
    private val filter = ForegroundFilter("com.example.screenshottagger", setOf("com.sohu.inputmethod.sogou"))

    @Test
    fun ignoresSystemWindows() {
        assertTrue(filter.ignore("com.android.systemui"))
        assertTrue(filter.ignore("com.miui.screenshot"))
        assertTrue(filter.ignore("com.sohu.inputmethod.sogou"))
    }

    @Test
    fun keepsNormalApps() {
        // 本应用包名里也含 "screenshot"，但不能被过滤
        assertFalse(filter.ignore("com.example.screenshottagger"))
        assertFalse(filter.ignore("com.tencent.mm"))
        assertFalse(filter.ignore("com.miui.home"))
    }
}
