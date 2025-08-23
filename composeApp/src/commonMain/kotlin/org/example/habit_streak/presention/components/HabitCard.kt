package org.example.habit_streak.presention.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.habit_streak.domain.model.Habit

@Composable
fun HabitCard(
    habit: Habit,
    isCompleted: Boolean,
    completedCount: Int,
    currentStreak: Int,
    onToggleCompletion: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) {
            HabitStreakTheme.habitColorToComposeColor(habit.color).copy(alpha = 0.1f)
        } else {
            HabitStreakTheme.surfaceColor
        },
        animationSpec = tween(300)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onCardClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion checkbox
            CompletionCheckbox(
                isCompleted = isCompleted,
                color = HabitStreakTheme.habitColorToComposeColor(habit.color),
                onClick = onToggleCompletion
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon
            HabitIconDisplay(
                icon = habit.icon,
                color = HabitStreakTheme.habitColorToComposeColor(habit.color),
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = HabitStreakTheme.primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (habit.targetCount > 1) {
                    Text(
                        text = "$completedCount / ${habit.targetCount} ${habit.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HabitStreakTheme.secondaryTextColor
                    )
                }
            }

            // Streak indicator
            if (currentStreak > 0) {
                StreakBadge(streak = currentStreak)
            }
        }
    }
}