package com.example.bricklyfrontend.data

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordUtils {
    /**
     * Хеширует пароль с помощью BCrypt с 1 итерацией (cost = 4, т.к. 2^4 = 16 ≈ 1 round)
     */
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(4, password.toCharArray())
    }
}
