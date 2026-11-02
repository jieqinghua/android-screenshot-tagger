package com.example.screenshottagger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileNamesTest {
    @Test
    fun appendsLabelBeforeExtension() {
        assertEquals("Screenshot_20261101_143005_微信.png", FileNames.tagged("Screenshot_20261101_143005.png", "微信"))
    }

    @Test
    fun skipsAlreadyTagged() {
        assertNull(FileNames.tagged("Screenshot_1_微信.png", "微信"))
    }

    @Test
    fun skipsBlankLabel() {
        assertNull(FileNames.tagged("a.png", " /:* "))
    }

    @Test
    fun stripsIllegalCharacters() {
        assertEquals("ab cd", FileNames.sanitize("a/b:\n  c*d?"))
    }

    @Test
    fun truncatesWithoutSplittingSurrogatePair() {
        val label = "a".repeat(39) + "😀"
        assertEquals("a".repeat(39), FileNames.sanitize(label))
    }

    @Test
    fun handlesNameWithoutExtension() {
        assertEquals("shot_QQ", FileNames.tagged("shot", "QQ"))
    }

    @Test
    fun addsNumericSuffix() {
        assertEquals("a_微信 (2).png", FileNames.withSuffix("a_微信.png", 2))
    }
}
