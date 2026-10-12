package com.shortsblocker.app.logging

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * On-device-only ring buffer of block decisions. Never stores screen text or
 * view content — only package name, matched rule id (or "UNMATCHED"), and a
 * timestamp — so a stale rule shows up as a report instead of silently
 * passing content through or wrongly blocking it.
 *
 * Writes are performed off the main thread to prevent UI stutter and StrictMode
 * violations. Unmatched entries are deduplicated and rate-limited so they do not
 * flush Block entries out of the ring buffer.
 */
class BlockEventLogger(
    private val prefs: SharedPreferences,
    private val diskExecutor: Executor = defaultExecutor,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultExecutor,
    )

    private val lastUnmatchedTimestamps = ConcurrentHashMap<String, Long>()
    @Volatile
    private var lastLoggedPackage: String? = null
    @Volatile
    private var lastLoggedResult: String? = null

    fun logBlock(packageName: String, ruleId: String) {
        lastLoggedPackage = packageName
        lastLoggedResult = ruleId
        enqueueAppend(packageName, ruleId)
    }

    fun logUnmatched(packageName: String) {
        val now = System.currentTimeMillis()
        val lastTime = lastUnmatchedTimestamps[packageName] ?: 0L
        if (now - lastTime < UNMATCHED_RATE_LIMIT_MS) {
            return
        }
        if (lastLoggedPackage == packageName && lastLoggedResult == "UNMATCHED") {
            return
        }
        lastUnmatchedTimestamps[packageName] = now
        lastLoggedPackage = packageName
        lastLoggedResult = "UNMATCHED"
        enqueueAppend(packageName, "UNMATCHED")
    }

    fun recentEvents(limit: Int = MAX_EVENTS): List<Event> = synchronized(this) {
        val jsonString = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(jsonString)
            (0 until array.length()).map { i ->
                val entry = array.getJSONObject(i)
                Event(entry.getLong("t"), entry.getString("pkg"), entry.getString("result"))
            }.takeLast(limit)
        }.getOrElse { emptyList() }
    }

    fun clear() {
        lastUnmatchedTimestamps.clear()
        lastLoggedPackage = null
        lastLoggedResult = null
        diskExecutor.execute {
            synchronized(this) {
                prefs.edit().putString(KEY_EVENTS, "[]").apply()
            }
        }
    }

    private fun enqueueAppend(packageName: String, result: String) {
        val now = System.currentTimeMillis()
        diskExecutor.execute {
            synchronized(this) {
                runCatching {
                    val array = JSONArray(prefs.getString(KEY_EVENTS, "[]") ?: "[]")
                    val entry = JSONObject()
                        .put("t", now)
                        .put("pkg", packageName)
                        .put("result", result)
                    array.put(entry)

                    val trimmed = JSONArray()
                    val start = maxOf(0, array.length() - MAX_EVENTS)
                    for (i in start until array.length()) {
                        trimmed.put(array.get(i))
                    }

                    prefs.edit().putString(KEY_EVENTS, trimmed.toString()).apply()
                }
            }
        }
    }

    data class Event(val timestampMillis: Long, val packageName: String, val result: String)

    companion object {
        private const val PREFS_NAME = "block_event_log"
        private const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 50
        const val UNMATCHED_RATE_LIMIT_MS = 5_000L

        private val defaultExecutor: Executor by lazy {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "block-event-logger").apply { isDaemon = true }
            }
        }
    }
}
