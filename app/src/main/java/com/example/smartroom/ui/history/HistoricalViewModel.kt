package com.example.smartroom.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.data.model.CurrentDataResponse
import com.example.smartroom.data.remote.RetrofitFactory
import kotlinx.coroutines.launch

// Holds all values rendered and edited on the History screen.
data class HistoricalUiState(
    val startDateTime: String = "",
    val endDateTime: String = "",
    val historicalData: List<CurrentDataResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)

// Loads historical sensor values from GET /api/data using the saved Raspberry Pi IP.
class HistoricalViewModel(
    private val localSettingsStore: LocalSettingsStore
) : ViewModel() {

    // Compose observes this state and redraws the History screen when it changes.
    var uiState by mutableStateOf(HistoricalUiState())
        private set

    // Updates the start datetime text field while the user types.
    fun onStartDateTimeChanged(newStartDateTime: String) {
        uiState = uiState.copy(startDateTime = newStartDateTime, errorMessage = "")
    }

    // Updates the end datetime text field while the user types.
    fun onEndDateTimeChanged(newEndDateTime: String) {
        uiState = uiState.copy(endDateTime = newEndDateTime, errorMessage = "")
    }

    // Calls the backend endpoint and stores the returned historical entries.
    fun loadHistoricalData() {
        val savedIpAddress = localSettingsStore.getIpAddress()

        if (savedIpAddress.isBlank()) {
            uiState = uiState.copy(errorMessage = "Set the Raspberry Pi IP in Settings first.")
            return
        }

        if (uiState.startDateTime.isBlank() || uiState.endDateTime.isBlank()) {
            uiState = uiState.copy(errorMessage = "Enter both start and end datetime values.")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            try {
                val apiService = RetrofitFactory.createApiService(savedIpAddress)
                val response = apiService.getHistoricalData(
                    start = uiState.startDateTime,
                    end = uiState.endDateTime
                )

                uiState = uiState.copy(historicalData = response, isLoading = false)
            } catch (_: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to load history. Check datetime format, IP, or Wi-Fi."
                )
            }
        }
    }
}

