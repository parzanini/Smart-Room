package com.example.smartroom.data.local

import android.content.Context
import android.content.SharedPreferences

class LocalSettingsStore(context: Context) {

    // This is the SharedPreferences file used by the app.
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Saves the Raspberry Pi IP address typed by the user.
    fun saveIpAddress(ipAddress: String) {
        preferences.edit().putString(KEY_IP_ADDRESS, ipAddress.trim()).apply()
    }

    // Returns the saved IP address, or an empty value when nothing was saved yet.
    fun getIpAddress(): String {
        return preferences.getString(KEY_IP_ADDRESS, "") ?: ""
    }

    // Saves the temperature safety range defined by the user.
    fun saveTemperatureRange(min: Float, max: Float) {
        preferences.edit()
            .putFloat(KEY_TEMP_MIN, min)
            .putFloat(KEY_TEMP_MAX, max)
            .apply()
    }

    // Saves the humidity safety range defined by the user.
    fun saveHumidityRange(min: Float, max: Float) {
        preferences.edit()
            .putFloat(KEY_HUMIDITY_MIN, min)
            .putFloat(KEY_HUMIDITY_MAX, max)
            .apply()
    }

    // Reads all configured limits and returns defaults for first app launch.
    fun getTemperatureRange(): Pair<Float, Float> {
        val min = preferences.getFloat(KEY_TEMP_MIN, 18f)
        val max = preferences.getFloat(KEY_TEMP_MAX, 28f)
        return Pair(min, max)
    }

    // Reads humidity limits and returns defaults for first app launch.
    fun getHumidityRange(): Pair<Float, Float> {
        val min = preferences.getFloat(KEY_HUMIDITY_MIN, 35f)
        val max = preferences.getFloat(KEY_HUMIDITY_MAX, 60f)
        return Pair(min, max)
    }

    companion object {
        private const val PREFERENCES_NAME = "smart_room_settings"
        private const val KEY_IP_ADDRESS = "ip_address"
        private const val KEY_TEMP_MIN = "temp_min"
        private const val KEY_TEMP_MAX = "temp_max"
        private const val KEY_HUMIDITY_MIN = "humidity_min"
        private const val KEY_HUMIDITY_MAX = "humidity_max"
    }
}


