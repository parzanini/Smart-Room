package com.example.smartroom.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Draws all local app settings that the user can change and save.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    onIpAddressChanged: (String) -> Unit,
    onTemperatureMinChanged: (Float) -> Unit,
    onTemperatureMaxChanged: (Float) -> Unit,
    onHumidityMinChanged: (Float) -> Unit,
    onHumidityMaxChanged: (Float) -> Unit,
    onDarkModeToggled: (Boolean) -> Unit,
    onSaveClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Settings") })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Connection
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = uiState.ipAddress,
                onValueChange = onIpAddressChanged,
                label = { Text("Raspberry Pi IP Address") },
                placeholder = { Text("e.g. 192.168.1.15") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Section: Thresholds
            Text(
                text = "Alert Thresholds",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            ElevatedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThresholdSlider(
                        label = "Temperature Min",
                        value = uiState.temperatureMin,
                        onValueChange = onTemperatureMinChanged,
                        valueRange = 0f..50f,
                        unit = "°C"
                    )
                    ThresholdSlider(
                        label = "Temperature Max",
                        value = uiState.temperatureMax,
                        onValueChange = onTemperatureMaxChanged,
                        valueRange = 0f..50f,
                        unit = "°C"
                    )
                    ThresholdSlider(
                        label = "Humidity Min",
                        value = uiState.humidityMin,
                        onValueChange = onHumidityMinChanged,
                        valueRange = 0f..100f,
                        unit = "%"
                    )
                    ThresholdSlider(
                        label = "Humidity Max",
                        value = uiState.humidityMax,
                        onValueChange = onHumidityMaxChanged,
                        valueRange = 0f..100f,
                        unit = "%"
                    )
                }
            }

            // Section: Appearance
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Dark Mode", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.isDarkModeEnabled,
                    onCheckedChange = onDarkModeToggled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save All Settings")
            }

            // Feedback Message
            if (uiState.feedbackMessage.isNotBlank()) {
                val feedbackColor = if (uiState.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = feedbackColor)
                    Text(
                        text = uiState.feedbackMessage,
                        color = feedbackColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ThresholdSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${value.toInt()}$unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
