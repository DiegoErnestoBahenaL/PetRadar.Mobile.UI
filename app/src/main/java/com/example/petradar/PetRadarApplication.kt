package com.example.petradar

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy
import com.example.petradar.api.RetrofitClient
import com.example.petradar.utils.AuthManager
import com.example.petradar.utils.NotificationHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PetRadarApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        NotificationHelper.createChannel(this)
        setupCoil()
    }

    /**
     * Configures a global Coil [ImageLoader] that injects the Bearer token into
     * every image request at request-time (lazy read), so the token is always
     * current even if it was refreshed after app start.
     */
    private fun setupCoil() {
        val context = this
        val coilHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Read token at request time — same pattern as RetrofitClient.
                val token = AuthManager.getAuthToken(context)
                val request = if (!token.isNullOrEmpty()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .okHttpClient(coilHttpClient)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        )
    }
}
