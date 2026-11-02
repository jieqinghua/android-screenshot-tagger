package com.example.screenshottagger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundHistoryTest {
    @Test
    fun returnsAppActiveAtGivenTime() {
        val h = ForegroundHistory()
        h.record(100, "a")
        h.record(200, "b")
        h.record(300, "c")
        assertEquals("a", h.appAt(150))
        assertEquals("b", h.appAt(200))
        assertEquals("c", h.appAt(10_000))
        assertNull(h.appAt(50))
    }

    @Test
    fun ignoresConsecutiveDuplicates() {
        val h = ForegroundHistory()
        h.record(100, "a")
        assertFalse(h.record(200, "a"))
        assertEquals("a", h.appAt(150))
    }

    @Test
    fun wrapsAroundWhenFull() {
        val h = ForegroundHistory(capacity = 3)
        for (i in 1..5) h.record(i * 100L, "p$i")
        assertEquals("p5", h.appAt(1_000))
        assertEquals("p3", h.appAt(350))
        assertNull(h.appAt(250)) // p1、p2 已被覆盖
    }
}
