package com.shortsblocker.app.logging

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device-only ring buffer of block decisions. Never stores screen text or
 * view content — only package name, matched rule id (or "UNMATCHED"), and a
 * timestamp — so a stale rule shows up as a report instead of silently
 * passing content through or wrongly blocking it.
 */
class BlockEventLogger(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun logBlock(packageName: String, ruleId: String) = append(packageName, ruleId)

    fun logUnmatched(packageName: String) = append(packageName, "UNMATCHED")

    fun recentEvents(limit: Int = MAX_EVENTS): List<Event> {
        val array = JSONArray(prefs.getString(KEY_EVENTS, "[]"))
        val events = (0 until array.length()).map { i ->
            val entry = array.getJSONObject(i)
            Event(entry.getLong("t"), entry.getString("pkg"), entry.getString("result"))
        }
        return events.takeLast(limit)
    }

    private fun append(packageName: String, result: String) {
        val array = JSONArray(prefs.getString(KEY_EVENTS, "[]"))
        val entry = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("pkg", packageName)
            .put("result", result)
        array.put(entry)

        val trimmed = JSONArray()
        val start = maxOf(0, array.length() - MAX_EVENTS)
        for (i in start until array.length()) trimmed.put(array.get(i))

        prefs.edit().putString(KEY_EVENTS, trimmed.toString()).apply()
    }

    data class Event(val timestampMillis: Long, val packageName: String, val result: String)

    companion object {
        private const val PREFS_NAME = "block_event_log"
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 50
    }
}
