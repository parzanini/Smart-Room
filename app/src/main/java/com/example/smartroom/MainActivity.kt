package com.example.smartroom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.notifications.NotificationHelper
import com.example.smartroom.ui.dashboard.DashboardScreen
import com.example.smartroom.ui.dashboard.DashboardViewModel
import com.example.smartroom.ui.settings.SettingsScreen
import com.example.smartroom.ui.settings.SettingsViewModel
import com.example.smartroom.ui.theme.SmartRoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prepares notifications and asks permission on Android 13+.
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()

        enableEdgeToEdge()

        setContent {
            // Creates the local storage object once for this composition.
            val localSettingsStore = remember { LocalSettingsStore(applicationContext) }

            // Creates a simple ViewModel instance that manages Settings screen state.
            val settingsViewModel = remember { SettingsViewModel(localSettingsStore) }

            // Creates the Dashboard ViewModel that reads current sensor data.
            val dashboardViewModel = remember {
                DashboardViewModel(
                    localSettingsStore = localSettingsStore,
                    onOutOfRangeDetected = { alertMessage ->
                        NotificationHelper.showOutOfRangeNotification(
                            context = applicationContext,
                            message = alertMessage
                        )
                    }
                )
            }

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
                        onStartPolling = dashboardViewModel::startPolling,
                        onStopPolling = dashboardViewModel::stopPolling,
                        onFanToggleClicked = dashboardViewModel::toggleFan,
                        onOpenSettingsClicked = { showSettingsScreen = true }
                    )
                }
            }
        }
    }

    // Requests POST_NOTIFICATIONS only on Android 13+, where it is required at runtime.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }
}
