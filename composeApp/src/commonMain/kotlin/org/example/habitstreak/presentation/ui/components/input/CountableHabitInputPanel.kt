package org.example.habitstreak.presentation.ui.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Input panel for countable habits (targetCount > 1)
 * Moved from HabitProgressInputPanel to common input package
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountableHabitInputPanel(
    currentValue: Int,
    targetCount: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit,
    onFillDay: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var sliderValue by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }

    // Quick select values - sabit değerler
    val quickSelectValues = listOf(1, 5, 10, 50, 100)

    // Seçili step değeri - varsayılan 1
    var selectedStep by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress gösterimi - 5 / 10 formatında
        Text(
            text = "$currentValue / $targetCount",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // +/- Buttons ve Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Minus Button
            FilledIconButton(
                onClick = {
                    val newValue = (currentValue - selectedStep).coerceAtLeast(0)
                    onValueChange(newValue)
                    sliderValue = newValue.toFloat()
                },
                enabled = currentValue > 0,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Slider - sadece slider, progress bar yok
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    val newValue = it.roundToInt().coerceIn(0, targetCount)
                    if (newValue != currentValue) {
                        onValueChange(newValue)
                    }
                },
                valueRange = 0f..targetCount.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor.copy(alpha = 0.7f),
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )

            // Plus Button - target geçse bile enabled kalır
            FilledIconButton(
                onClick = {
                    val newValue = currentValue + selectedStep
                    onValueChange(newValue)
                    sliderValue = newValue.coerceAtMost(targetCount).toFloat() // Slider max target'ta kalır
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Connected Button Group
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                quickSelectValues.forEachIndexed { index, value ->
                    val isSelected = selectedStep == value
                    val isFirst = index == 0
                    val isLast = index == quickSelectValues.lastIndex

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(
                                when {
                                    isFirst -> RoundedCornerShape(
                                        topStart = 6.dp,
                                        bottomStart = 6.dp,
                                        topEnd = 0.dp,
                                        bottomEnd = 0.dp
                                    )
                                    isLast -> RoundedCornerShape(
                                        topStart = 0.dp,
                                        bottomStart = 0.dp,
                                        topEnd = 6.dp,
                                        bottomEnd = 6.dp
                                    )
                                    else -> RoundedCornerShape(0.dp)
                                }
                            )
                            .background(
                                if (isSelected) accentColor else Color.Transparent
                            )
                            .clickable { selectedStep = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.toString(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sıfırla Button
            OutlinedButton(
                onClick = {
                    onReset()
                    sliderValue = 0f
                },
                modifier = Modifier.weight(1f),
                enabled = currentValue > 0,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Sıfırla")
            }

            // Günü Doldur Button - target geçince disabled
            FilledTonalButton(
                onClick = {
                    onFillDay()
                    sliderValue = targetCount.toFloat()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor
                ),
                enabled = currentValue < targetCount,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Günü Doldur")
            }
        }
    }
}