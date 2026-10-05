package com.secretcalc.browser

import android.content.Context

object PinStore {
    const val PREFS = "secret"
    const val KEY_CODE = "code"
    const val DEFAULT_CODE = "000000"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            prefs.getString(KEY_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
        } catch (_: ClassCastException) {
            val old = prefs.getInt(KEY_CODE, 0)
            val migrated = old.toString().padStart(6, '0')
            set(context, migrated)
            migrated
        }
    }

    fun set(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CODE, code)
            .apply()
    }

    fun isValid(code: String): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }
}
