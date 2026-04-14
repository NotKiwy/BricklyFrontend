package com.example.bricklyfrontend.data

import android.content.Context
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://195.133.49.96:8080"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun makeApi(u: String, p: String): ApiService {
        val authInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", Credentials.basic(u, p))
                    .build()
            )
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Volatile
    private var _currentUsername: String = ""
    @Volatile
    private var _currentPassword: String = ""

    val api: ApiService
        get() = makeApi(_currentUsername, _currentPassword)

    fun apiWithCredentials(username: String, password: String): ApiService =
        makeApi(username, password)

    fun setCredentials(username: String, password: String) {
        _currentUsername = username
        _currentPassword = password
    }

    fun clearCredentials() {
        _currentUsername = ""
        _currentPassword = ""
    }

    fun init(context: Context) {
        _currentUsername = UserPreferences.getUsername(context)
        _currentPassword = UserPreferences.getPassword(context)
    }
}
