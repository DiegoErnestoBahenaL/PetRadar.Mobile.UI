package com.example.petradar

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy
import com.example.petradar.api.RetrofitClient
import com.example.petradar.utils.NotificationHelper

class PetRadarApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        NotificationHelper.createChannel(this)
        setupCoil()
    }

    /**
     * Configures a global Coil [ImageLoader] that shares the same [OkHttpClient]
     * used by Retrofit. This ensures:
     *  - Every image request includes the `Authorization: Bearer <token>` header.
     *  - Expired tokens are automatically refreshed via the Authenticator (same
     *    token-refresh logic as API calls), so images never stop loading after
     *    a token expiry.
     */
    private fun setupCoil() {
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(RetrofitClient.imageHttpClient)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        )
    }
}
