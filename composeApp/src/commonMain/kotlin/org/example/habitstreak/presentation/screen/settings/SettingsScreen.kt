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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.core.util.AppLocale

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
                    val currentAppLocale = AppLocale.current()
                    val settingsText = stringResource(Res.string.nav_settings)
                    println("📱 SettingsScreen: Title recomposing with locale ${currentAppLocale.code}, text: '$settingsText'")
                    Text(
                        text = settingsText,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.nav_back))
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
                // General Settings
                item {
                    SettingsSection(title = stringResource(Res.string.section_general)) {
                        SettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = stringResource(Res.string.settings_theme),
                            subtitle = when(uiState.theme) {
                                "system" -> stringResource(Res.string.theme_system)
                                "light" -> stringResource(Res.string.theme_light)
                                "dark" -> stringResource(Res.string.theme_dark)
                                else -> stringResource(Res.string.theme_system)
                            },
                            onClick = { showThemeDialog = true }
                        )
                        LanguageSettingsItem(
                            currentLocale = uiState.locale,
                            onLocaleChanged = { locale ->
                                viewModel.setLocale(locale)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.Archive,
                            title = stringResource(Res.string.setting_archived_habits),
                            subtitle = stringResource(Res.string.setting_archived_habits_desc),
                            onClick = onNavigateToArchivedHabits
                        )
                    }
                }

                // Notifications Section
                item {
                    SettingsSection(title = stringResource(Res.string.section_notifications)) {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(Res.string.setting_push_notifications),
                            subtitle = stringResource(Res.string.setting_push_notifications_desc),
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
                                    title = stringResource(Res.string.setting_sound),
                                    subtitle = stringResource(Res.string.setting_sound_desc),
                                    checked = uiState.soundEnabled,
                                    onCheckedChange = { viewModel.toggleSound(it) },
                                    enabled = !uiState.isLoading
                                )
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Vibration,
                                    title = stringResource(Res.string.setting_vibration),
                                    subtitle = stringResource(Res.string.setting_vibration_desc),
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
                    SettingsSection(title = stringResource(Res.string.section_data_privacy)) {
                        SettingsItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = stringResource(Res.string.setting_backup_restore),
                            subtitle = stringResource(Res.string.setting_backup_restore_desc),
                            onClick = onNavigateToBackup
                        )
                        SettingsItem(
                            icon = Icons.Outlined.FileDownload,
                            title = stringResource(Res.string.setting_export_data),
                            subtitle = stringResource(Res.string.setting_export_data_desc),
                            onClick = { }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSection(title = stringResource(Res.string.section_about)) {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(Res.string.settings_about),
                            subtitle = stringResource(Res.string.setting_version),
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

@Composable
private fun LanguageSettingsItem(
    currentLocale: AppLocale,
    onLocaleChanged: (AppLocale) -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    SettingsItem(
        icon = Icons.Outlined.Language,
        title = stringResource(Res.string.settings_language),
        subtitle = currentLocale.displayName,
        onClick = { showLanguageDialog = true }
    )

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLocale = currentLocale,
            onDismiss = { showLanguageDialog = false },
            onLocaleSelected = { locale ->
                onLocaleChanged(locale)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLocale: AppLocale,
    onDismiss: () -> Unit,
    onLocaleSelected: (AppLocale) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_language)) },
        text = {
            Column {
                AppLocale.entries.forEach { locale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLocaleSelected(locale) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLocale == locale,
                            onClick = { onLocaleSelected(locale) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = locale.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}