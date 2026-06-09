package com.example.bricklyfrontend.data

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREFS_NAME = "brickly_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_ROLE = "role"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(context: Context, id: Long, username: String, password: String = "", role: String = "ROLE_USER") {
        prefs(context).edit()
            .putLong(KEY_USER_ID, id)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUserId(context: Context): Long =
        prefs(context).getLong(KEY_USER_ID, -1L)

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun getPassword(context: Context): String =
        prefs(context).getString(KEY_PASSWORD, "") ?: ""

    fun getRole(context: Context): String =
        prefs(context).getString(KEY_ROLE, "ROLE_USER") ?: "ROLE_USER"

    private fun roles(context: Context): List<String> =
        getRole(context).split(",").map { it.trim() }

    fun isSuperAdmin(context: Context): Boolean =
        "ROLE_SUPERADMIN" in roles(context)

    fun isAdmin(context: Context): Boolean {
        val r = roles(context)
        return "ROLE_ADMIN" in r || "ROLE_SUPERADMIN" in r
    }

    fun isMeetingCreator(context: Context): Boolean {
        val r = roles(context)
        return "ROLE_MEETING_CREATOR" in r || "ROLE_ADMIN" in r || "ROLE_SUPERADMIN" in r
    }

    fun isSeller(context: Context): Boolean {
        val r = roles(context)
        return "ROLE_SELLER" in r || "ROLE_ADMIN" in r || "ROLE_SUPERADMIN" in r
    }

    fun extractRole(authorities: List<AuthorityShortDTO>?): String =
        authorities?.map { it.authority }?.joinToString(",") ?: "ROLE_USER"

    fun isLoggedIn(context: Context): Boolean =
        getUserId(context) != -1L

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
