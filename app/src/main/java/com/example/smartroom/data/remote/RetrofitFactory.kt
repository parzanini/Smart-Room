package com.example.smartroom.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    // Creates a Retrofit API instance using the IP address saved by the user.
    fun createApiService(ipAddress: String): SmartRoomApiService {
        val normalizedIp = ipAddress.trim()

        // Flask runs on port 5000 in this project.
        val baseUrl = "http://$normalizedIp:5000/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartRoomApiService::class.java)
    }
}

