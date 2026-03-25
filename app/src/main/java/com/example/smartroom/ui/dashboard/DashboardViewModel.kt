package com.example.smartroom.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.data.model.CurrentDataResponse
import com.example.smartroom.data.remote.RetrofitFactory
import kotlinx.coroutines.launch

// Holds everything the Dashboard screen needs to render sensor state.
data class DashboardUiState(
    val currentData: CurrentDataResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)

// Fetches current sensor data from the Raspberry Pi using the saved IP address.
class DashboardViewModel(
    private val localSettingsStore: LocalSettingsStore
) : ViewModel() {

    // Compose observes this state and updates the Dashboard automatically.
    var uiState by mutableStateOf(DashboardUiState())
        private set

    init {
        loadCurrentData()
    }

    // Requests the latest temperature and humidity values from /api/current.
    fun loadCurrentData() {
        val savedIpAddress = localSettingsStore.getIpAddress()

        if (savedIpAddress.isBlank()) {
            uiState = uiState.copy(
                errorMessage = "Set the Raspberry Pi IP in Settings first.",
                isLoading = false
            )
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            try {
                val apiService = RetrofitFactory.createApiService(savedIpAddress)
                val response = apiService.getCurrentData()
                uiState = uiState.copy(currentData = response, isLoading = false)
            } catch (_: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to read sensor data. Check IP or Wi-Fi connection."
                )
            }
        }
    }
}

