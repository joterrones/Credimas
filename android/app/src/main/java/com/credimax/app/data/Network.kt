package com.credimax.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.credimax.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore("credimax")

class TokenStore(private val context: Context) {
    private val key = stringPreferencesKey("jwt")

    val token: Flow<String?> = context.dataStore.data.map { it[key] }

    suspend fun save(token: String) {
        context.dataStore.edit { it[key] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }

    fun current(): String? = runBlocking { token.first() }
}

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)
    val gson = Gson()

    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore.current()
        val req = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(req)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)
}

fun parseApiError(gson: Gson, raw: String?): String {
    return try {
        val parsed = gson.fromJson(raw, ApiError::class.java)
        val base = parsed?.error ?: "Error de red"
        val detail = raw?.let {
            if (it.contains("\"detail\"")) it.take(240) else null
        }
        if (detail != null) "$base | $detail" else base
    } catch (_: Exception) {
        raw?.take(180) ?: "Error de red"
    }
}
