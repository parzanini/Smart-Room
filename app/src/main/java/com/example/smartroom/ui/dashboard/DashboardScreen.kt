package com.example.smartroom.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Shows the latest temperature and humidity values returned by the backend.
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    uiState: DashboardUiState,
    onRefreshClicked: () -> Unit,
    onStartPolling: () -> Unit,
    onStopPolling: () -> Unit,
    onFanToggleClicked: () -> Unit
) {
    // Starts polling while this Composable is on screen and stops it when leaving.
    DisposableEffect(Unit) {
        onStartPolling()
        onDispose {
            onStopPolling()
        }
    }

    Column(
        modifier = modifier
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

        // Shows a centered primary action for fan control.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = onFanToggleClicked,
                enabled = !uiState.isUpdatingActuator,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .width(240.dp)
                    .padding(vertical = 8.dp)
            ) {
                val fanButtonLabel = if (uiState.isFanOn) "Turn Fan OFF" else "Turn Fan ON"
                Text(text = fanButtonLabel, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Shows backend response or actuator update errors.
        if (uiState.actuatorMessage.isNotBlank()) {
            Text(text = uiState.actuatorMessage)
        }
    }
}

