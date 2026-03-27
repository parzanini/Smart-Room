package com.example.smartroom.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Shows the latest temperature and humidity values returned by the backend.
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefreshClicked: () -> Unit,
    onStartPolling: () -> Unit,
    onStopPolling: () -> Unit,
    onFanToggleClicked: () -> Unit,
    onOpenSettingsClicked: () -> Unit
) {
    // Starts polling while this Composable is on screen and stops it when leaving.
    DisposableEffect(Unit) {
        onStartPolling()
        onDispose {
            onStopPolling()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Dashboard", style = MaterialTheme.typography.headlineSmall)

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        if (uiState.errorMessage.isNotBlank()) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        val currentData = uiState.currentData
        if (currentData != null) {
            Text(text = "Temperature: ${currentData.temperature} C")
            Text(text = "Humidity: ${currentData.humidity}%")
            Text(text = "Timestamp: ${currentData.timestamp}")
        }

        Button(onClick = onRefreshClicked, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh")
        }

        // Sends a fan ON/OFF command to the backend actuator endpoint.
        Button(
            onClick = onFanToggleClicked,
            enabled = !uiState.isUpdatingActuator,
            modifier = Modifier.fillMaxWidth()
        ) {
            val fanButtonLabel = if (uiState.isFanOn) "Turn fan off" else "Turn fan on"
            Text(fanButtonLabel)
        }

        // Shows backend response or actuator update errors.
        if (uiState.actuatorMessage.isNotBlank()) {
            Text(text = uiState.actuatorMessage)
        }

        Button(onClick = onOpenSettingsClicked, modifier = Modifier.fillMaxWidth()) {
            Text("Open settings")
        }
    }
}

