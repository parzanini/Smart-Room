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
    val selectedRange: Pair<Long?, Long?> = Pair(null, null),
    val rangeLabel: String = "Select Date Range",
    val historicalData: List<CurrentDataResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    // Summary statistics
    val avgTemp: Float? = null,
    val maxTemp: Float? = null,
    val avgHum: Float? = null,
    val maxHum: Float? = null
)

// Loads historical sensor values from GET /api/data using the saved Raspberry Pi IP.
class HistoricalViewModel(
    private val localSettingsStore: LocalSettingsStore
) : ViewModel() {

    // Compose observes this state and redraws the History screen when it changes.
    var uiState by mutableStateOf(HistoricalUiState())
        private set

    // Updates the selected date range from the Material 3 DateRangePicker.
    fun onRangeSelected(startMillis: Long?, endMillis: Long?) {
        val label = if (startMillis != null && endMillis != null) {
            "${formatDateLabel(startMillis)} - ${formatDateLabel(endMillis)}"
        } else {
            "Select Date Range"
        }
        uiState = uiState.copy(
            selectedRange = Pair(startMillis, endMillis),
            rangeLabel = label,
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

        val (start, end) = uiState.selectedRange

        if (start == null || end == null) {
            uiState = uiState.copy(errorMessage = "Please select a date range.")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            try {
                val startQuery = toRangeStartIso(start)
                val endQuery = toRangeEndIso(end)
                val apiService = RetrofitFactory.createApiService(savedIpAddress)
                val response = apiService.getHistoricalData(
                    start = startQuery,
                    end = endQuery
                )

                // Calculate summary stats
                val temps = response.map { it.temperature.toFloat() }
                val hums = response.map { it.humidity.toFloat() }
                
                uiState = uiState.copy(
                    historicalData = response,
                    isLoading = false,
                    avgTemp = if (temps.isNotEmpty()) temps.average().toFloat() else null,
                    maxTemp = temps.maxOrNull(),
                    avgHum = if (hums.isNotEmpty()) hums.average().toFloat() else null,
                    maxHum = hums.maxOrNull()
                )
            } catch (_: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to load history. Check IP or Wi-Fi."
                )
            }
        }
    }

    // Formats calendar milliseconds into a short date label for the UI.
    private fun formatDateLabel(dateMillis: Long?): String {
        if (dateMillis == null) return ""
        val formatter = SimpleDateFormat("MMM dd", Locale.UK)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(dateMillis))
    }

    private fun toRangeStartIso(dateMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return "${formatter.format(Date(dateMillis))}T00:00:00Z"
    }

    private fun toRangeEndIso(dateMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return "${formatter.format(Date(dateMillis))}T23:59:59Z"
    }
}
