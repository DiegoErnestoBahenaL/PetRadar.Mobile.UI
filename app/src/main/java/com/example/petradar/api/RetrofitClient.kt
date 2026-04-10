package com.example.petradar.api

import android.content.Context
import com.example.petradar.api.models.LoginResponse
import com.example.petradar.api.models.RefreshTokenRequest
import com.example.petradar.utils.AuthManager
import com.example.petradar.utils.JwtUtils
import com.google.gson.GsonBuilder
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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

    private val tokenRefreshLock = Any()

    private val refreshHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.endsWith("/api/gate/Login") || path.endsWith("/api/gate/Login/refresh")
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun refreshAccessTokenBlocking(context: Context): String? {
        val currentRefreshToken = AuthManager.getRefreshToken(context) ?: return null
        val bodyJson = gsonNoNulls.toJson(RefreshTokenRequest(currentRefreshToken))
        val requestBody = bodyJson
            .toRequestBody("application/json; charset=UTF-8".toMediaType())

        val refreshRequest = Request.Builder()
            .url("${BASE_URL}api/gate/Login/refresh")
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .build()

        return runCatching {
            refreshHttpClient.newCall(refreshRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                val rawBody = response.body.string().orEmpty()
                if (rawBody.isBlank()) return null

                val loginResponse = gsonNoNulls.fromJson(rawBody, LoginResponse::class.java)
                val newToken = loginResponse?.token?.takeIf { it.isNotBlank() } ?: return null
                val updatedRefreshToken = loginResponse.refreshToken ?: currentRefreshToken
                AuthManager.saveAuthToken(context, newToken, updatedRefreshToken)
                newToken
            }
        }.getOrNull()
    }

    /**
     * Dedicated OkHttpClient for loading images via Coil.
     *  - Adds Authorization header unconditionally (even if token is technically
     *    near-expiry) so the server always receives credentials.
     *  - The Authenticator handles 401 responses by refreshing the token and
     *    retrying, exactly like the main client.
     *  - Does NOT add `Accept: application/json` — image endpoints return binary.
     *  - Includes logging so image HTTP traffic is visible in Logcat (tag OkHttp).
     */
    val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                appContextRef.get()?.let { ctx ->
                    val token = AuthManager.getAuthToken(ctx)
                    // Always send the token if present; Authenticator handles refresh on 401
                    if (!token.isNullOrEmpty()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(requestBuilder.build())
            }
            // Strip conditional-GET headers so the server always receives a fresh request.
            // Some QA backends crash (500) when they receive If-Modified-Since / If-None-Match
            // instead of responding correctly with 304 Not Modified.
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .removeHeader("If-Modified-Since")
                    .removeHeader("If-None-Match")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    val ctx = appContextRef.get() ?: return null
                    if (isAuthEndpoint(response.request)) return null
                    if (responseCount(response) >= 2) return null
                    synchronized(tokenRefreshLock) {
                        val latestToken = AuthManager.getAuthToken(ctx)
                        val requestToken = response.request.header("Authorization")
                            ?.removePrefix("Bearer ")?.trim()
                        if (!latestToken.isNullOrBlank() && latestToken != requestToken && !JwtUtils.isTokenExpired(latestToken)) {
                            return response.request.newBuilder()
                                .header("Authorization", "Bearer $latestToken")
                                .build()
                        }
                        val refreshedToken = refreshAccessTokenBlocking(ctx)
                        if (refreshedToken.isNullOrBlank()) {
                            AuthManager.logout(ctx)
                            return null
                        }
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $refreshedToken")
                            .build()
                    }
                }
            })
            .build()
    }

    /**
     * HTTP client configured with:
     *  - 30-second timeouts for connect, read and write.
     *  - Logging interceptor.
     *  - Authentication interceptor: adds the `Authorization: Bearer <token>` header
     *    to every request if a token is stored in [AuthManager].
     *  - Authenticator for automatic token refresh on 401 responses.
     *
     * Created lazily (only when first needed).
     */
    val okHttpClient: OkHttpClient by lazy {
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
                    if (!token.isNullOrEmpty() && !JwtUtils.isTokenExpired(token)) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }

                chain.proceed(requestBuilder.build())
            }
            .authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    val ctx = appContextRef.get() ?: return null
                    if (isAuthEndpoint(response.request)) return null
                    if (responseCount(response) >= 2) return null

                    synchronized(tokenRefreshLock) {
                        val latestToken = AuthManager.getAuthToken(ctx)
                        val requestToken = response.request.header("Authorization")
                            ?.removePrefix("Bearer ")
                            ?.trim()

                        if (!latestToken.isNullOrBlank() && latestToken != requestToken && !JwtUtils.isTokenExpired(latestToken)) {
                            return response.request.newBuilder()
                                .header("Authorization", "Bearer $latestToken")
                                .build()
                        }

                        val refreshedToken = refreshAccessTokenBlocking(ctx)
                        if (refreshedToken.isNullOrBlank()) {
                            AuthManager.logout(ctx)
                            return null
                        }

                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $refreshedToken")
                            .build()
                    }
                }
            })
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
