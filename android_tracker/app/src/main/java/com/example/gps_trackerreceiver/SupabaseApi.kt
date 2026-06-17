package com.example.gps_trackerreceiver

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query //Retrofit turns complex HTTP requests into simple java/kotlin interface calls

interface SupabaseApi {
    @GET("rest/v1/locations") //Perform HTTP GET request, value inside () is the endpoint
    suspend fun getLatestLocation( //Suspend kotlin from execution, go and do other things and come back when data is ready
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("device_id") deviceId: String,
        @Query("order") order: String = "created_at.desc",//Newest data at the top
        @Query("limit") limit: Int = 1 //Send back only 1 row, after = is the default value
    ): List<LocationResponse>
}