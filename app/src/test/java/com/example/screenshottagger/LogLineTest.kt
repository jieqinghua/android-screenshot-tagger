package com.example.screenshottagger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class LogLineTest {
    @Test
    fun findsTagAndTime() {
        val line = LogLine.parse("Screenshot_20261101_143005.png → Screenshot_20261101_143005_微信.png (com.tencent.mm)")
        assertEquals("Screenshot_20261101_143005_微信.png", line.newName)
        assertEquals("_微信", line.newName.substring(line.tagStart, line.tagEnd))
        assertEquals(LocalDateTime.of(2026, 11, 1, 14, 30, 5), line.time)
    }

    @Test
    fun excludesDuplicateSuffixFromTag() {
        val line = LogLine.parse("Screenshot_20261101_143005.png → Screenshot_20261101_143005_微信 (1).png (com.tencent.mm)")
        assertEquals("_微信", line.newName.substring(line.tagStart, line.tagEnd))
    }

    @Test
    fun keepsUnknownLineAsIs() {
        val line = LogLine.parse("something else")
        assertEquals("something else", line.newName)
        assertFalse(line.hasTag)
        assertNull(line.time)
    }

    @Test
    fun noTimeWhenNameHasNone() {
        val line = LogLine.parse("shot.png → shot_QQ.png (com.tencent.mobileqq)")
        assertEquals("_QQ", line.newName.substring(line.tagStart, line.tagEnd))
        assertNull(line.time)
    }
}
