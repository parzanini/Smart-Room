package com.example.smartroom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartroom.data.local.LocalSettingsStore
import com.example.smartroom.notifications.NotificationHelper
import com.example.smartroom.ui.dashboard.DashboardScreen
import com.example.smartroom.ui.dashboard.DashboardViewModel
import com.example.smartroom.ui.history.HistoricalScreen
import com.example.smartroom.ui.history.HistoricalViewModel
import com.example.smartroom.ui.settings.SettingsScreen
import com.example.smartroom.ui.settings.SettingsViewModel
import com.example.smartroom.ui.theme.SmartRoomTheme

private enum class AppScreen {
    SETTINGS,
    DASHBOARD,
    HISTORY
}

class MainActivity : ComponentActivity() {

    // Moves the store to the class level to ensure it's created once for the activity.
    private lateinit var localSettingsStore: LocalSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localSettingsStore = LocalSettingsStore(applicationContext)

        // Prepares notifications and asks permission on Android 13+.
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()

        enableEdgeToEdge()

        setContent {
            // Uses the standard ViewModel factory to ensure lifecycle persistence.
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(localSettingsStore) as T
                    }
                }
            )

            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return DashboardViewModel(
                            localSettingsStore = localSettingsStore,
                            onOutOfRangeDetected = { alertMessage ->
                                NotificationHelper.showOutOfRangeNotification(
                                    context = applicationContext,
                                    message = alertMessage
                                )
                            }
                        ) as T
                    }
                }
            )

            val historicalViewModel: HistoricalViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return HistoricalViewModel(localSettingsStore) as T
                    }
                }
            )

            // Re-evaluates setup status whenever the IP address changes in the store.
            var hasCompletedSetup by remember {
                mutableStateOf(localSettingsStore.getIpAddress().isNotBlank()) 
            }

            // Opens Settings first only when no IP was saved yet.
            var currentScreen by remember {
                mutableStateOf(
                    if (hasCompletedSetup) AppScreen.DASHBOARD else AppScreen.SETTINGS
                )
            }

            // Shows a one-time onboarding message when required settings are still missing.
            var showSettingsRequiredDialog by remember {
                mutableStateOf(!hasCompletedSetup)
            }

            SmartRoomTheme(
                darkTheme = settingsViewModel.uiState.isDarkModeEnabled,
                dynamicColor = false
            ) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = {
                                    if (hasCompletedSetup) {
                                        currentScreen = AppScreen.DASHBOARD
                                    } else {
                                        currentScreen = AppScreen.SETTINGS
                                        showSettingsRequiredDialog = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Dashboard") },
                                enabled = hasCompletedSetup
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HISTORY,
                                onClick = {
                                    if (hasCompletedSetup) {
                                        currentScreen = AppScreen.HISTORY
                                    } else {
                                        currentScreen = AppScreen.SETTINGS
                                        showSettingsRequiredDialog = true
                                    }
                                },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                label = { Text("History") },
                                enabled = hasCompletedSetup
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SETTINGS,
                                onClick = { currentScreen = AppScreen.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        AppScreen.SETTINGS -> {
                            if (showSettingsRequiredDialog) {
                                AlertDialog(
                                    onDismissRequest = { },
                                    title = { Text("Complete settings first") },
                                    text = {
                                        Text("To use Smart Room, please enter your Raspberry Pi IP and save your settings first.")
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showSettingsRequiredDialog = false
                                        }) {
                                            Text("Understood")
                                        }
                                    }
                                )
                            }

                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                uiState = settingsViewModel.uiState,
                                onIpAddressChanged = settingsViewModel::onIpAddressChanged,
                                onTemperatureMinChanged = settingsViewModel::onTemperatureMinChanged,
                                onTemperatureMaxChanged = settingsViewModel::onTemperatureMaxChanged,
                                onHumidityMinChanged = settingsViewModel::onHumidityMinChanged,
                                onHumidityMaxChanged = settingsViewModel::onHumidityMaxChanged,
                                onDarkModeToggled = settingsViewModel::onDarkModeToggled,
                                onSaveClicked = {
                                    settingsViewModel.saveSettings()
                                    if (!settingsViewModel.uiState.isError) {
                                        hasCompletedSetup = true
                                        currentScreen = AppScreen.DASHBOARD
                                        dashboardViewModel.loadCurrentData()
                                    }
                                }
                            )
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                modifier = Modifier.padding(innerPadding),
                                uiState = dashboardViewModel.uiState,
                                onRefreshClicked = dashboardViewModel::loadCurrentData,
                                onStartPolling = dashboardViewModel::startPolling,
                                onStopPolling = dashboardViewModel::stopPolling,
                                onFanToggleClicked = dashboardViewModel::toggleFan
                            )
                        }

                        AppScreen.HISTORY -> {
                            HistoricalScreen(
                                modifier = Modifier.padding(innerPadding),
                                uiState = historicalViewModel.uiState,
                                onStartDateSelected = historicalViewModel::onStartDateSelected,
                                onEndDateSelected = historicalViewModel::onEndDateSelected,
                                onLoadHistoryClicked = historicalViewModel::loadHistoricalData
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permissionStatus = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        )

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }
}
