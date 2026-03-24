package com.example.smartroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.ui.settings.SettingsScreen
import com.example.smartroom.ui.settings.SettingsViewModel
import com.example.smartroom.ui.theme.SmartRoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Creates the local storage object once for this composition.
            val localSettingsStore = remember { LocalSettingsStore(applicationContext) }

            // Creates a simple ViewModel instance that manages Settings screen state.
            val settingsViewModel = remember { SettingsViewModel(localSettingsStore) }

            SmartRoomTheme {
                // Shows the Settings screen and forwards all UI events to the ViewModel.
                SettingsScreen(
                    uiState = settingsViewModel.uiState,
                    onIpAddressChanged = settingsViewModel::onIpAddressChanged,
                    onTemperatureMinChanged = settingsViewModel::onTemperatureMinChanged,
                    onTemperatureMaxChanged = settingsViewModel::onTemperatureMaxChanged,
                    onHumidityMinChanged = settingsViewModel::onHumidityMinChanged,
                    onHumidityMaxChanged = settingsViewModel::onHumidityMaxChanged,
                    onSaveClicked = settingsViewModel::saveSettings
                )
            }
        }
    }
}
