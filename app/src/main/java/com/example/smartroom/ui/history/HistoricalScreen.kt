package com.example.smartroom.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Displays historical sensor records returned by GET /api/data.
@Composable
fun HistoricalScreen(
    uiState: HistoricalUiState,
    onStartDateTimeChanged: (String) -> Unit,
    onEndDateTimeChanged: (String) -> Unit,
    onLoadHistoryClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "History", style = MaterialTheme.typography.headlineSmall)

        // Uses ISO datetime text so students can match Flask query values directly.
        OutlinedTextField(
            value = uiState.startDateTime,
            onValueChange = onStartDateTimeChanged,
            label = { Text("Start (example: 2026-03-28T10:00:00Z)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.endDateTime,
            onValueChange = onEndDateTimeChanged,
            label = { Text("End (example: 2026-03-28T15:00:00Z)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onLoadHistoryClicked, modifier = Modifier.fillMaxWidth()) {
            Text("Load history")
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        if (uiState.errorMessage.isNotBlank()) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        // Lists every data point with temperature, humidity, and timestamp.
        for (entry in uiState.historicalData) {
            Text(text = "Temperature: ${entry.temperature} C")
            Text(text = "Humidity: ${entry.humidity}%")
            Text(text = "Timestamp: ${entry.timestamp}")
            Text(text = "-------------------------")
        }

        Button(onClick = onBackClicked, modifier = Modifier.fillMaxWidth()) {
            Text("Back to dashboard")
        }
    }
}

