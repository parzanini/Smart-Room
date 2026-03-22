package com.example.smartroom.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.smartroom.data.local.LocalSettingsStore

// Holds all values shown and edited in the Settings screen.
data class SettingsUiState(
    val ipAddress: String = "",
    val temperatureMin: Float = 18f,
    val temperatureMax: Float = 28f,
    val humidityMin: Float = 35f,
    val humidityMax: Float = 60f
)

// Connects LocalSettingsStore to Compose state so the UI can read and update settings.
class SettingsViewModel(
    private val localSettingsStore: LocalSettingsStore
) : ViewModel() {

    // Compose observes this property and redraws the Settings screen automatically.
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        loadSettingsFromStorage()
    }

    // Reads all settings from SharedPreferences when the ViewModel starts.
    private fun loadSettingsFromStorage() {
        val savedTemperatureRange = localSettingsStore.getTemperatureRange()
        val savedHumidityRange = localSettingsStore.getHumidityRange()

        uiState = uiState.copy(
            ipAddress = localSettingsStore.getIpAddress(),
            temperatureMin = savedTemperatureRange.first,
            temperatureMax = savedTemperatureRange.second,
            humidityMin = savedHumidityRange.first,
            humidityMax = savedHumidityRange.second
        )
    }

    // Updates only the IP text field value while the user is typing.
    fun onIpAddressChanged(newIpAddress: String) {
        uiState = uiState.copy(ipAddress = newIpAddress)
    }

    // Saves all editable settings in one place when the user taps Save.
    fun saveSettings() {
        localSettingsStore.saveIpAddress(uiState.ipAddress)
        localSettingsStore.saveTemperatureRange(uiState.temperatureMin, uiState.temperatureMax)
        localSettingsStore.saveHumidityRange(uiState.humidityMin, uiState.humidityMax)
    }
}

