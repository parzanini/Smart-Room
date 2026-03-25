package com.example.smartroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.ui.dashboard.DashboardScreen
import com.example.smartroom.ui.dashboard.DashboardViewModel
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

            // Creates the Dashboard ViewModel that reads current sensor data.
            val dashboardViewModel = remember { DashboardViewModel(localSettingsStore) }

            // Opens Settings first if there is no saved IP address yet.
            var showSettingsScreen by remember {
                mutableStateOf(settingsViewModel.uiState.ipAddress.isBlank())
            }

            SmartRoomTheme {
                if (showSettingsScreen) {
                    // Shows the Settings screen and forwards all UI events to the ViewModel.
                    SettingsScreen(
                        uiState = settingsViewModel.uiState,
                        onIpAddressChanged = settingsViewModel::onIpAddressChanged,
                        onTemperatureMinChanged = settingsViewModel::onTemperatureMinChanged,
                        onTemperatureMaxChanged = settingsViewModel::onTemperatureMaxChanged,
                        onHumidityMinChanged = settingsViewModel::onHumidityMinChanged,
                        onHumidityMaxChanged = settingsViewModel::onHumidityMaxChanged,
                        onSaveClicked = {
                            settingsViewModel.saveSettings()
                            if (!settingsViewModel.uiState.isError) {
                                showSettingsScreen = false
                                dashboardViewModel.loadCurrentData()
                            }
                        }
                    )
                } else {
                    // Shows live sensor values and allows users to refresh or reopen Settings.
                    DashboardScreen(
                        uiState = dashboardViewModel.uiState,
                        onRefreshClicked = dashboardViewModel::loadCurrentData,
                        onOpenSettingsClicked = { showSettingsScreen = true }
                    )
                }
            }
        }
    }
}
