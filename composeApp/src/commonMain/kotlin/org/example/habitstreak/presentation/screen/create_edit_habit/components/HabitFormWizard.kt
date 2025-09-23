package org.example.habitstreak.presentation.screen.create_edit_habit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.nav_back
import habitstreak.composeapp.generated.resources.action_save
import org.jetbrains.compose.resources.stringResource

/**
 * Wizard component for habit creation/editing following Single Responsibility Principle.
 * Handles only step navigation and progress display.
 */
@Composable
fun HabitFormWizard(
    currentStep: Int,
    totalSteps: Int,
    stepProgress: Float,
    onStepChange: (Int) -> Unit,
    onSave: () -> Unit,
    isEditMode: Boolean,
    content: @Composable (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { stepProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step content with animation
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                val slideDirection = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { it * slideDirection },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it * slideDirection },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.weight(1f),
            label = "step_content"
        ) { step ->
            content(step)
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { onStepChange(currentStep - 1) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                    Text(stringResource(Res.string.nav_back))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Next/Save button
            if (currentStep < totalSteps - 1) {
                Button(
                    onClick = { onStepChange(currentStep + 1) }
                ) {
                    Text("Continue")
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            } else {
                Button(
                    onClick = onSave
                ) {
                    Text(
                        stringResource(
                            if (isEditMode) Res.string.action_save
                            else Res.string.action_save
                        )
                    )
                }
            }
        }
    }
}