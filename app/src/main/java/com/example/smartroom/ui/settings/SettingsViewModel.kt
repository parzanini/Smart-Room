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
    val humidityMax: Float = 60f,
    val isDarkModeEnabled: Boolean = false,
    val feedbackMessage: String = "",
    val isError: Boolean = false
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
            humidityMax = savedHumidityRange.second,
            isDarkModeEnabled = localSettingsStore.isDarkModeEnabled()
        )
    }

    // Updates only the IP text field value while the user is typing.
    fun onIpAddressChanged(newIpAddress: String) {
        uiState = uiState.copy(ipAddress = newIpAddress, feedbackMessage = "", isError = false)
    }

    // Updates only the minimum allowed temperature selected by the user.
    fun onTemperatureMinChanged(newTemperatureMin: Float) {
        uiState =
            uiState.copy(temperatureMin = newTemperatureMin, feedbackMessage = "", isError = false)
    }

    // Updates only the maximum allowed temperature selected by the user.
    fun onTemperatureMaxChanged(newTemperatureMax: Float) {
        uiState =
            uiState.copy(temperatureMax = newTemperatureMax, feedbackMessage = "", isError = false)
    }

    // Updates only the minimum allowed humidity selected by the user.
    fun onHumidityMinChanged(newHumidityMin: Float) {
        uiState = uiState.copy(humidityMin = newHumidityMin, feedbackMessage = "", isError = false)
    }

    // Updates only the maximum allowed humidity selected by the user.
    fun onHumidityMaxChanged(newHumidityMax: Float) {
        uiState = uiState.copy(humidityMax = newHumidityMax, feedbackMessage = "", isError = false)
    }

    // Updates the theme toggle state chosen by the user.
    fun onDarkModeToggled(isDarkModeEnabled: Boolean) {
        uiState = uiState.copy(
            isDarkModeEnabled = isDarkModeEnabled,
            feedbackMessage = "",
            isError = false
        )
    }

    // Checks if the user-entered settings are valid before saving.
    private fun validateInputs(): String? {
        if (uiState.ipAddress.isBlank()) {
            return "Please enter the Raspberry Pi IP address."
        }

        if (uiState.temperatureMin > uiState.temperatureMax) {
            return "Temperature min cannot be greater than max."
        }

        if (uiState.humidityMin > uiState.humidityMax) {
            return "Humidity min cannot be greater than max."
        }

        return null
    }

    // Saves all editable settings in one place when the user taps Save.
    fun saveSettings() {
        val validationError = validateInputs()
        if (validationError != null) {
            uiState = uiState.copy(feedbackMessage = validationError, isError = true)
            return
        }

        // Uses the consolidated save method to ensure all fields are written to disk.
        localSettingsStore.saveAllSettings(
            ipAddress = uiState.ipAddress,
            tempMin = uiState.temperatureMin,
            tempMax = uiState.temperatureMax,
            humidityMin = uiState.humidityMin,
            humidityMax = uiState.humidityMax,
            isDarkMode = uiState.isDarkModeEnabled
        )

        uiState = uiState.copy(feedbackMessage = "Settings saved successfully.", isError = false)
    }
}
