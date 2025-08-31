package org.example.habitstreak.presentation.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.habitstreak.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToArchivedHabits: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section
                item {
                    ProfileSection()
                }

                // General Settings
                item {
                    SettingsSection(title = "General") {
                        SettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = "Theme",
                            subtitle = when(uiState.theme) {
                                "system" -> "System default"
                                "light" -> "Light"
                                "dark" -> "Dark"
                                else -> "System default"
                            },
                            onClick = { showThemeDialog = true }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.Language,
                            title = "Language",
                            subtitle = "English",
                            onClick = { }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.Archive,
                            title = "Archived Habits",
                            subtitle = "View and restore archived habits",
                            onClick = onNavigateToArchivedHabits
                        )
                    }
                }

                // Notifications Section
                item {
                    SettingsSection(title = "Notifications") {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Push Notifications",
                            subtitle = "Get reminders for your habits",
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleNotifications(enabled)
                            },
                            enabled = !uiState.isLoading
                        )

                        AnimatedVisibility(visible = uiState.notificationsEnabled) {
                            Column {
                                SettingsSwitchItem(
                                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                    title = "Sound",
                                    subtitle = "Play sound with notifications",
                                    checked = uiState.soundEnabled,
                                    onCheckedChange = { viewModel.toggleSound(it) },
                                    enabled = !uiState.isLoading
                                )
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Vibration,
                                    title = "Vibration",
                                    subtitle = "Vibrate with notifications",
                                    checked = uiState.vibrationEnabled,
                                    onCheckedChange = { viewModel.toggleVibration(it) },
                                    enabled = !uiState.isLoading
                                )
                            }
                        }
                    }
                }

                // Data & Privacy
                item {
                    SettingsSection(title = "Data & Privacy") {
                        SettingsItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = "Backup & Restore",
                            subtitle = "Backup your data to cloud",
                            onClick = onNavigateToBackup
                        )
                        SettingsItem(
                            icon = Icons.Outlined.FileDownload,
                            title = "Export Data",
                            subtitle = "Export as CSV or JSON",
                            onClick = { }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSection(title = "About") {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "About",
                            subtitle = "Version 1.0.0",
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.theme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
                showThemeDialog = false
            }
        )
    }
}