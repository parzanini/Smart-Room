package com.example.smartroom.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.data.model.ActuatorRequest
import com.example.smartroom.data.model.CurrentDataResponse
import com.example.smartroom.data.remote.RetrofitFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Holds everything the Dashboard screen needs to render sensor state.
data class DashboardUiState(
    val currentData: CurrentDataResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isFanOn: Boolean = false,
    val isUpdatingActuator: Boolean = false,
    val actuatorMessage: String = ""
)

// Fetches current sensor data from the Raspberry Pi using the saved IP address.
class DashboardViewModel(
    private val localSettingsStore: LocalSettingsStore,
    private val onOutOfRangeDetected: (String) -> Unit = {}
) : ViewModel() {

    // Compose observes this state and updates the Dashboard automatically.
    var uiState by mutableStateOf(DashboardUiState())
        private set

    // Holds the repeating polling coroutine while Dashboard is visible.
    private var pollingJob: Job? = null

    // Prevents repeated notifications while values stay continuously outside limits.
    private var hasActiveRangeAlert = false

    init {
        loadCurrentData()
    }

    // Requests the latest temperature and humidity values from /api/current.
    fun loadCurrentData() {
        viewModelScope.launch {
            requestCurrentData()
        }
    }

    // Performs one network request cycle and updates loading, success, and error states.
    private suspend fun requestCurrentData() {
        val savedIpAddress = localSettingsStore.getIpAddress()

        if (savedIpAddress.isBlank()) {
            uiState = uiState.copy(
                errorMessage = "Set the Raspberry Pi IP in Settings first.",
                isLoading = false
            )
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = "")

        try {
            val apiService = RetrofitFactory.createApiService(savedIpAddress)
            val response = apiService.getCurrentData()
            uiState = uiState.copy(currentData = response, isLoading = false)
            checkSafeRanges(response)
        } catch (_: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Failed to read sensor data. Check IP or Wi-Fi connection."
            )
        }
    }

    // Compares the latest values with saved ranges and emits an alert message if needed.
    private fun checkSafeRanges(currentData: CurrentDataResponse) {
        val temperatureRange = localSettingsStore.getTemperatureRange()
        val humidityRange = localSettingsStore.getHumidityRange()

        val isTemperatureOutside =
            currentData.temperature < temperatureRange.first || currentData.temperature > temperatureRange.second
        val isHumidityOutside =
            currentData.humidity < humidityRange.first || currentData.humidity > humidityRange.second

        if (!isTemperatureOutside && !isHumidityOutside) {
            hasActiveRangeAlert = false
            return
        }

        if (hasActiveRangeAlert) {
            return
        }

        val warningParts = mutableListOf<String>()
        if (isTemperatureOutside) {
            warningParts.add("temperature ${currentData.temperature} C")
        }
        if (isHumidityOutside) {
            warningParts.add("humidity ${currentData.humidity}%")
        }

        onOutOfRangeDetected("Out of safe range: ${warningParts.joinToString(" and ")}")
        hasActiveRangeAlert = true
    }

    // Starts repeated GET /api/current requests while the Dashboard screen is open.
    fun startPolling(intervalMillis: Long = 5000L) {
        if (pollingJob?.isActive == true) {
            return
        }

        pollingJob = viewModelScope.launch {
            while (isActive) {
                requestCurrentData()
                delay(intervalMillis)
            }
        }
    }

    // Stops the polling loop when the Dashboard screen is closed.
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // Sends an ON/OFF command to the backend for the fan actuator.
    fun toggleFan() {
        val savedIpAddress = localSettingsStore.getIpAddress()

        if (savedIpAddress.isBlank()) {
            uiState = uiState.copy(actuatorMessage = "Set the Raspberry Pi IP in Settings first.")
            return
        }

        val targetState = !uiState.isFanOn
        uiState = uiState.copy(isUpdatingActuator = true, actuatorMessage = "")

        viewModelScope.launch {
            try {
                val apiService = RetrofitFactory.createApiService(savedIpAddress)
                val response = apiService.updateActuator(
                    ActuatorRequest(device = "fan", state = targetState)
                )

                uiState = uiState.copy(
                    isFanOn = targetState,
                    isUpdatingActuator = false,
                    actuatorMessage = response.message
                )
            } catch (_: Exception) {
                uiState = uiState.copy(
                    isUpdatingActuator = false,
                    actuatorMessage = "Failed to update fan state. Check IP or Wi-Fi connection."
                )
            }
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

