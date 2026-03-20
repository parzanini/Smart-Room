package com.example.smartroom.data.remote

import com.example.smartroom.data.model.ActuatorRequest
import com.example.smartroom.data.model.ActuatorResponse
import com.example.smartroom.data.model.CurrentDataResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SmartRoomApiService {

    // Reads the latest temperature and humidity values from the Raspberry Pi.
    @GET("api/current")
    suspend fun getCurrentData(): CurrentDataResponse

    // Sends an ON/OFF command for the selected actuator device.
    @POST("api/actuator")
    suspend fun updateActuator(@Body request: ActuatorRequest): ActuatorResponse

    // Reads historical data points from an optional date/time range.
    @GET("api/data")
    suspend fun getHistoricalData(
        @Query("start") start: String,
        @Query("end") end: String
    ): List<CurrentDataResponse>
}

