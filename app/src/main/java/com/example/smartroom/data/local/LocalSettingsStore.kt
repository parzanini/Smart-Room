package com.example.smartroom.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class LocalSettingsStore(context: Context) {

    // This is the SharedPreferences file used by the app.
    // Mode Private ensures only this app can read it.
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Saves all settings at once using a single editor to ensure persistence.
    // Returns true if the save was successful.
    fun saveAllSettings(
        ipAddress: String,
        tempMin: Float,
        tempMax: Float,
        humidityMin: Float,
        humidityMax: Float,
        isDarkMode: Boolean
    ): Boolean {
        Log.d("LocalSettingsStore", "Saving IP: $ipAddress")

        return preferences.edit()
            .putString(KEY_IP_ADDRESS, ipAddress.trim())
            .putFloat(KEY_TEMP_MIN, tempMin)
            .putFloat(KEY_TEMP_MAX, tempMax)
            .putFloat(KEY_HUMIDITY_MIN, humidityMin)
            .putFloat(KEY_HUMIDITY_MAX, humidityMax)
            .putBoolean(KEY_DARK_MODE_ENABLED, isDarkMode)
            .commit() // Using commit() instead of apply() to ensure synchronous disk write.
    }

    // Returns the saved IP address, or an empty value when nothing was saved yet.
    fun getIpAddress(): String {
        val ip = preferences.getString(KEY_IP_ADDRESS, "") ?: ""
        Log.d("LocalSettingsStore", "Reading IP: $ip")
        return ip
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

    // Returns the stored theme preference. Default is light mode (false).
    fun isDarkModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
    }

    companion object {
        private const val PREFERENCES_NAME =
            "smart_room_settings_v1" // Changed name to ensure a clean state
        private const val KEY_IP_ADDRESS = "ip_address"
        private const val KEY_TEMP_MIN = "temp_min"
        private const val KEY_TEMP_MAX = "temp_max"
        private const val KEY_HUMIDITY_MIN = "humidity_min"
        private const val KEY_HUMIDITY_MAX = "humidity_max"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
    }
}
