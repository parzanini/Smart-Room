package com.example.smartroom.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Draws all local app settings that the user can change and save.
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onIpAddressChanged: (String) -> Unit,
    onTemperatureMinChanged: (Float) -> Unit,
    onTemperatureMaxChanged: (Float) -> Unit,
    onHumidityMinChanged: (Float) -> Unit,
    onHumidityMaxChanged: (Float) -> Unit,
    onSaveClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen title to help beginners understand where they are.
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)

        // Lets the user define which Raspberry Pi IP the app should call.
        OutlinedTextField(
            value = uiState.ipAddress,
            onValueChange = onIpAddressChanged,
            label = { Text("Raspberry Pi IP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Temperature range controls.
        Text(text = "Temperature min: ${uiState.temperatureMin.toInt()} C")
        Slider(
            value = uiState.temperatureMin,
            onValueChange = onTemperatureMinChanged,
            valueRange = 0f..50f
        )

        Text(text = "Temperature max: ${uiState.temperatureMax.toInt()} C")
        Slider(
            value = uiState.temperatureMax,
            onValueChange = onTemperatureMaxChanged,
            valueRange = 0f..50f
        )

        // Humidity range controls.
        Text(text = "Humidity min: ${uiState.humidityMin.toInt()}%")
        Slider(
            value = uiState.humidityMin,
            onValueChange = onHumidityMinChanged,
            valueRange = 0f..100f
        )

        Text(text = "Humidity max: ${uiState.humidityMax.toInt()}%")
        Slider(
            value = uiState.humidityMax,
            onValueChange = onHumidityMaxChanged,
            valueRange = 0f..100f
        )

        // Persists all values at once in SharedPreferences.
        Button(onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()) {
            Text("Save settings")
        }
    }
}

