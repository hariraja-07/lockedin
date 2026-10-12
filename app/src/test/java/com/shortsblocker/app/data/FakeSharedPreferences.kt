package com.shortsblocker.app.data

import android.content.SharedPreferences

class FakeSharedPreferences(
    private val data: MutableMap<String, Any?> = mutableMapOf(),
) : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = data

    override fun getString(
        key: String?,
        defValue: String?,
    ): String? = (data[key] as? String) ?: defValue

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (data[key] as? MutableSet<String>)
            ?: defValues

    override fun getInt(
        key: String?,
        defValue: Int,
    ): Int = (data[key] as? Int) ?: defValue

    override fun getLong(
        key: String?,
        defValue: Long,
    ): Long = (data[key] as? Long) ?: defValue

    override fun getFloat(
        key: String?,
        defValue: Float,
    ): Float = (data[key] as? Float) ?: defValue

    override fun getBoolean(
        key: String?,
        defValue: Boolean,
    ): Boolean = (data[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(
            key: String?,
            value: String?,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = values
            return this
        }

        override fun putInt(
            key: String?,
            value: Int,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putLong(
            key: String?,
            value: Long,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putFloat(
            key: String?,
            value: Float,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putBoolean(
            key: String?,
            value: Boolean,
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = this
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) data.clear()
            pending.forEach { (k, v) ->
                if (v === this) data.remove(k) else data[k] = v
            }
            pending.clear()
        }
    }
}
