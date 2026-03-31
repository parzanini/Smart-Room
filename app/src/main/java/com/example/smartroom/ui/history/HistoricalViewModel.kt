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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Holds all values rendered and edited on the History screen.
data class HistoricalUiState(
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val startDateLabel: String = "",
    val endDateLabel: String = "",
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

    // Updates the selected start date from the calendar picker.
    fun onStartDateSelected(newStartDateMillis: Long?) {
        uiState = uiState.copy(
            startDateMillis = newStartDateMillis,
            startDateLabel = formatDateLabel(newStartDateMillis),
            errorMessage = ""
        )
    }

    // Updates the selected end date from the calendar picker.
    fun onEndDateSelected(newEndDateMillis: Long?) {
        uiState = uiState.copy(
            endDateMillis = newEndDateMillis,
            endDateLabel = formatDateLabel(newEndDateMillis),
            errorMessage = ""
        )
    }

    // Calls the backend endpoint and stores the returned historical entries.
    fun loadHistoricalData() {
        val savedIpAddress = localSettingsStore.getIpAddress()

        if (savedIpAddress.isBlank()) {
            uiState = uiState.copy(errorMessage = "Set the Raspberry Pi IP in Settings first.")
            return
        }

        val selectedStartDateMillis = uiState.startDateMillis
        val selectedEndDateMillis = uiState.endDateMillis

        if (selectedStartDateMillis == null || selectedEndDateMillis == null) {
            uiState = uiState.copy(errorMessage = "Select both start and end dates.")
            return
        }

        if (selectedStartDateMillis > selectedEndDateMillis) {
            uiState = uiState.copy(errorMessage = "Start date cannot be after end date.")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            try {
                val startQuery = toRangeStartIso(selectedStartDateMillis)
                val endQuery = toRangeEndIso(selectedEndDateMillis)
                val apiService = RetrofitFactory.createApiService(savedIpAddress)
                val response = apiService.getHistoricalData(
                    start = startQuery,
                    end = endQuery
                )

                uiState = uiState.copy(historicalData = response, isLoading = false)
            } catch (_: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to load history. Check selected dates, IP, or Wi-Fi."
                )
            }
        }
    }

    // Formats calendar milliseconds into a short date label for the UI.
    private fun formatDateLabel(dateMillis: Long?): String {
        if (dateMillis == null) {
            return ""
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(dateMillis))
    }

    // Converts a selected date to the first second of the day in UTC.
    private fun toRangeStartIso(dateMillis: Long): String {
        return "${formatDateLabel(dateMillis)}T00:00:00Z"
    }

    // Converts a selected date to the last second of the day in UTC.
    private fun toRangeEndIso(dateMillis: Long): String {
        return "${formatDateLabel(dateMillis)}T23:59:59Z"
    }
}

