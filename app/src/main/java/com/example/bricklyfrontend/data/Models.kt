package com.example.bricklyfrontend.data

import com.google.gson.annotations.SerializedName

data class UserCreateDTO(
    val username: String,
    val password: String,
    val createdAt: String
)

data class UserUpdateDTO(
    val username: String? = null,
    val name: String? = null,
    val password: String? = null,
    val email: String? = null,
    val city: String? = null
)

data class UserDefaultDTO(
    val id: Long,
    val username: String,
    val name: String?,
    val createdAt: String?,
    val email: String?,
    val city: String?
)

data class UserFullDTO(
    val id: Long,
    val username: String,
    val name: String?,
    val password: String?,
    val createdAt: String?,
    val email: String?,
    val city: String?
)

data class UserShortDTO(
    val id: Long,
    val username: String,
    val name: String?
)
