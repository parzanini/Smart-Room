package com.example.smartroom.data.model

// Represents the current sensor snapshot returned by GET /api/current.
data class CurrentDataResponse(
    val temperature: Double,
    val humidity: Double,
    val timestamp: String
)

// Represents the payload sent to POST /api/actuator.
data class ActuatorRequest(
    val device: String,
    val state: Boolean
)

// Represents the response returned by POST /api/actuator.
data class ActuatorResponse(
    val status: String,
    val message: String
)

