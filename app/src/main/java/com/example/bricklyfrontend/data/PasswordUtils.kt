package com.example.bricklyfrontend.data

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordUtils {
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(4, password.toCharArray())
    }
}
