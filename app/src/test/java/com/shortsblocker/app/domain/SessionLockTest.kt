package com.shortsblocker.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLockTest {

    private var currentTime = 1_000_000L
    private val lock = SessionLock(timeProvider = { currentTime }, timeoutMs = 30_000L)

    @Test
    fun `starts locked by default`() {
        assertFalse(lock.isUnlocked())
    }

    @Test
    fun `unlock marks session as unlocked`() {
        lock.unlock()
        assertTrue(lock.isUnlocked())
    }

    @Test
    fun `lock marks session as locked`() {
        lock.unlock()
        assertTrue(lock.isUnlocked())
        lock.lock()
        assertFalse(lock.isUnlocked())
    }

    @Test
    fun `backgrounding for less than timeout maintains unlocked state`() {
        lock.unlock()
        lock.onAppBackgrounded()

        // Advance 10 seconds
        currentTime += 10_000L
        lock.onAppForegrounded()

        assertTrue(lock.isUnlocked())
    }

    @Test
    fun `backgrounding for equal or greater than timeout locks session`() {
        lock.unlock()
        lock.onAppBackgrounded()

        // Advance 30 seconds
        currentTime += 30_000L
        lock.onAppForegrounded()

        assertFalse(lock.isUnlocked())
    }

    @Test
    fun `backgrounding when already locked remains locked`() {
        lock.onAppBackgrounded()
        currentTime += 50_000L
        lock.onAppForegrounded()
        assertFalse(lock.isUnlocked())
    }
}
