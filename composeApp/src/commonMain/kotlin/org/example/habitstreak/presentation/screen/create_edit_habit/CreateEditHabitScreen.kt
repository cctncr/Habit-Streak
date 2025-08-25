package org.example.habitstreak.presentation.screen.create_edit_habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.example.habitstreak.domain.model.*
import org.example.habitstreak.presentation.screen.create_edit_habit.components.FrequencySelectionDialog
import org.example.habitstreak.presentation.screen.create_edit_habit.components.SelectionCard
import org.example.habitstreak.presentation.screen.create_edit_habit.components.getFrequencyDisplayText
import org.example.habitstreak.presentation.ui.components.selection.ColorSelectionGrid
import org.example.habitstreak.presentation.ui.components.selection.IconSelectionGrid
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.navigationBarsPadding
import org.example.habitstreak.presentation.viewmodel.CreateEditHabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditHabitScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateEditHabitViewModel = koinViewModel { parametersOf(habitId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showIconSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Edit Habit" else "Create Habit")
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveHabit(onSuccess = onNavigateBack)
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Habit Name") },
                placeholder = { Text("e.g., Drink Water") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null && uiState.title.isBlank(),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description (Optional)") },
                placeholder = { Text("Why is this habit important?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // Icon and Color Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Selection Button
                SelectionCard(
                    modifier = Modifier.weight(1f),
                    title = "Icon",
                    onClick = { showIconSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = HabitStreakTheme.habitColorToComposeColor(uiState.selectedColor)
                                    .copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.selectedIcon.emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Color Selection Button
                SelectionCard(
                    modifier = Modifier.weight(1f),
                    title = "Color",
                    onClick = { showColorSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = HabitStreakTheme.habitColorToComposeColor(uiState.selectedColor),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                }
            }

            // Frequency Selection
            SelectionCard(
                title = "Frequency",
                onClick = { showFrequencyDialog = true }
            ) {
                Text(
                    text = getFrequencyDisplayText(uiState.frequency),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Target Count (Optional)
            AnimatedVisibility(visible = uiState.frequency == HabitFrequency.Daily) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Daily Goal (Optional)",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = if (uiState.targetCount > 1) uiState.targetCount.toString() else "",
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { viewModel.updateTargetCount(it) }
                                },
                                label = { Text("Count") },
                                placeholder = { Text("1") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = uiState.unit,
                                onValueChange = viewModel::updateUnit,
                                label = { Text("Unit") },
                                placeholder = { Text("times") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Error Message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Icon Selection Bottom Sheet
    if (showIconSheet) {
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Choose Icon",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                IconSelectionGrid(
                    selectedIcon = uiState.selectedIcon,
                    selectedColor = uiState.selectedColor,
                    onIconSelected = { icon ->
                        viewModel.selectIcon(icon)
                        showIconSheet = false
                    },
                    modifier = Modifier.height(400.dp)
                )
            }
        }
    }

    // Color Selection Bottom Sheet
    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Choose Color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ColorSelectionGrid(
                    selectedColor = uiState.selectedColor,
                    onColorSelected = { color ->
                        viewModel.selectColor(color)
                        showColorSheet = false
                    },
                    modifier = Modifier.height(300.dp)
                )
            }
        }
    }

    // Frequency Selection Dialog
    if (showFrequencyDialog) {
        FrequencySelectionDialog(
            currentFrequency = uiState.frequency,
            onFrequencySelected = { frequency ->
                viewModel.updateFrequency(frequency)
                showFrequencyDialog = false
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }
}