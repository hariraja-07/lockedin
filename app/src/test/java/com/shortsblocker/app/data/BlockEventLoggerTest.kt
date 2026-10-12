package com.shortsblocker.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executor

class BlockEventLoggerTest {
    private val directExecutor = Executor { it.run() }
    private val prefs = FakeSharedPreferences()
    private val logger = BlockEventLogger(prefs, directExecutor)

    @Test
    fun `logBlock writes block entry`() {
        logger.logBlock("com.google.android.youtube", "yt_shorts_player")
        val events = logger.recentEvents()
        assertEquals(1, events.size)
        assertEquals("com.google.android.youtube", events[0].packageName)
        assertEquals("yt_shorts_player", events[0].result)
    }

    @Test
    fun `consecutive identical unmatched entries are deduplicated`() {
        logger.logUnmatched("com.instagram.android")
        logger.logUnmatched("com.instagram.android")
        val events = logger.recentEvents()
        assertEquals(1, events.size)
    }

    @Test
    fun `block entries are never rate limited or dropped by unmatched`() {
        logger.logBlock("com.google.android.youtube", "yt_shorts_player")
        logger.logBlock("com.google.android.youtube", "yt_shorts_player")
        val events = logger.recentEvents()
        assertEquals(2, events.size)
    }

    @Test
    fun `ring buffer truncates to max events`() {
        for (i in 1..60) {
            logger.logBlock("com.example.pkg", "rule_$i")
        }
        val events = logger.recentEvents()
        assertEquals(BlockEventLogger.MAX_EVENTS, events.size)
        assertEquals("rule_11", events.first().result)
        assertEquals("rule_60", events.last().result)
    }
}
