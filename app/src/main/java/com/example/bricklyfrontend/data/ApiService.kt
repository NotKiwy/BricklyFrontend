package com.example.bricklyfrontend.data

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("/api/app/users/register")
    suspend fun registerUser(@Body dto: UserCreateDTO): Response<UserDefaultDTO>

    @GET("/api/app/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserDefaultDTO>

    @GET("/api/app/users/exists/{username}")
    suspend fun checkUserExistence(@Path("username") username: String): Response<String>

    @PUT("/api/app/users/update/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body dto: UserUpdateDTO
    ): Response<UserFullDTO>
}
