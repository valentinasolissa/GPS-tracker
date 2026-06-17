package com.example.gps_trackerreceiver

import com.google.gson.annotations.SerializedName

data class LocationResponse( //Bridge from cloud data and android kotlin app
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("raw_payload")
    val rawPayload: String, // The Base64 string from Python

    @SerializedName("created_at")
    val createdAt: String
)