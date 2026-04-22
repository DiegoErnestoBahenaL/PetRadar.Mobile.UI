package com.example.petradar.api.models

import com.google.gson.annotations.SerializedName

data class UpdateFcmTokenRequest (
    @SerializedName("fcmToken")
    val fcmToken: String? = null
)
