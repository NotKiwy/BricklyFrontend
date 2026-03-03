package com.example.bricklyfrontend.data

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREFS_NAME = "brickly_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(context: Context, id: Long, username: String) {
        prefs(context).edit()
            .putLong(KEY_USER_ID, id)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getUserId(context: Context): Long =
        prefs(context).getLong(KEY_USER_ID, -1L)

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun isLoggedIn(context: Context): Boolean =
        getUserId(context) != -1L

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
