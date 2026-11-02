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
        assertTrue(filter.ignore("com.example.screenshottagger"))
    }

    @Test
    fun keepsNormalApps() {
        assertFalse(filter.ignore("com.tencent.mm"))
        assertFalse(filter.ignore("com.miui.home"))
    }
}
