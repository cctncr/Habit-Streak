package org.example.habitstreak.presentation.screen.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.habitstreak.presentation.ui.components.card.HabitCard
import org.example.habitstreak.presentation.ui.components.empty.EmptyHabitsState
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onNavigateToCreateHabit: () -> Unit,
    onNavigateToHabitDetail: (String) -> Unit,
    viewModel: HabitsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habitsWithCompletion by viewModel.habitsWithCompletion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HabitStreak") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HabitStreakTheme.backgroundColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateHabit,
                containerColor = HabitStreakTheme.successColor
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
        containerColor = HabitStreakTheme.backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                habitsWithCompletion.isEmpty() -> {
                    EmptyHabitsState(
                        onCreateHabit = onNavigateToCreateHabit
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(habitsWithCompletion) { habitWithCompletion ->
                            HabitCard(
                                habit = habitWithCompletion.habit,
                                isCompleted = habitWithCompletion.isCompletedToday,
                                completedCount = habitWithCompletion.completedCount,
                                currentStreak = uiState.streaks[habitWithCompletion.habit.id] ?: 0,
                                onToggleCompletion = {
                                    viewModel.toggleHabitCompletion(habitWithCompletion.habit.id)
                                },
                                onCardClick = {
                                    onNavigateToHabitDetail(habitWithCompletion.habit.id)
                                }
                            )
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}