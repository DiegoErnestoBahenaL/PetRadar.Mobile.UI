package com.example.petradar.api

import android.content.Context
import com.example.petradar.utils.AuthManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the configured [ApiService] instance for the entire app.
 *
 * Provides HTTP client with logging, auth headers, and Retrofit configuration.
 *
 * WeakReference avoids memory leaks; token resolved at request time for freshness.
 *
 * Call [init] in Application.onCreate before making network requests.
 */
object RetrofitClient {

    /** Base URL of the PetRadar API (QA environment). */
    const val BASE_URL = "https://api-qa.petradar-qa.org/"

    /**
     * Weak reference to the Application context.
     * Used only to read the authentication token per request.
     * WeakReference prevents retaining the context if the process dies.
     */
    private var appContextRef: WeakReference<Context> = WeakReference(null)

    /**
     * Initializes the client with the application context.
     * Must be called in [com.example.petradar.PetRadarApplication.onCreate].
     *
     * @param appContext Application context (NOT an Activity context).
     */
    fun init(appContext: Context) {
        appContextRef = WeakReference(appContext.applicationContext)
    }

    /**
     * OkHttp logging interceptor.
     * Level BODY: logs the URL, headers and body of each request/response.
     * Useful for debugging; in production this should be set to NONE.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /** Gson instance without null serialization (omits null fields from JSON output). */
    private val gsonNoNulls = GsonBuilder().create()

    /**
     * HTTP client configured with:
     *  - 30-second timeouts for connect, read and write.
     *  - Logging interceptor.
     *  - Authentication interceptor: adds the `Authorization: Bearer <token>` header
     *    to every request if a token is stored in [AuthManager].
     *
     * Created lazily (only when first needed).
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")

                // Resolve the token at request time to always use the most up-to-date value.
                appContextRef.get()?.let { ctx ->
                    val token = AuthManager.getAuthToken(ctx)
                    if (!token.isNullOrEmpty()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }

                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    /**
     * Retrofit instance with the base URL and the configured HTTP client.
     * Lazy: built only when first accessed.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gsonNoNulls))
            .build()
    }

    /**
     * Retrofit-generated implementation of the [ApiService] interface.
     * This is the instance used by all repositories to make HTTP requests.
     * Lazy: created only the first time it is accessed.
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
