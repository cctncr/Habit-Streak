package org.example.habitstreak.presentation.ui.components.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate

@Composable
fun HabitGrid(
    completedDates: Map<LocalDate, Float>, // Date -> progress (0-1 for percentage)
    startDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
    rows: Int = 3,
    boxSize: Dp = 28.dp,
    spacing: Dp = 3.dp,
    cornerRadius: Dp = 4.dp,
    maxHistoryDays: Long = 90L,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDateClick: (LocalDate) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Grid'i bugünden başlatıp geriye doğru göstereceğiz
    // En fazla maxHistoryDays kadar geriye gidebiliriz
    val earliestDate = maxOf(
        startDate,
        LocalDate.fromEpochDays(today.toEpochDays() - maxHistoryDays.toInt())
    )

    // Toplam gün sayısını hesapla
    val totalDays = (today.toEpochDays() - earliestDate.toEpochDays() + 1).toInt()
    val totalColumns = ((totalDays + rows - 1) / rows).coerceAtLeast(1)

    // İlk açılışta en sağa (bugüne) scroll et
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Biraz bekle ve sonra scroll et
            delay(100)
            if (totalColumns > 0) {
                listState.animateScrollToItem(totalColumns - 1)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        reverseLayout = false // Soldan sağa düzen
    ) {
        items(count = totalColumns) { columnIndex ->
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Bu kolondaki günleri hesapla (yukarıdan aşağıya)
                val firstDateInColumn = LocalDate.fromEpochDays(
                    earliestDate.toEpochDays() + (columnIndex * rows)
                )

                // Ay başlığı kontrolü
                var showMonthHeader = false
                var monthDate: LocalDate? = null

                // Bu kolondaki tüm tarihleri kontrol et
                for (rowIndex in 0 until rows) {
                    val date = LocalDate.fromEpochDays(
                        firstDateInColumn.toEpochDays() + rowIndex
                    )
                    if (date <= today && date >= earliestDate && date.day == 1) {
                        showMonthHeader = true
                        monthDate = date
                        break
                    }
                }

                // Ay başlığı veya boşluk
                if (showMonthHeader && monthDate != null) {
                    Text(
                        text = getMonthAbbreviation(monthDate.month.number),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(14.dp)
                            .padding(horizontal = 2.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Tarih hücreleri (yukarıdan aşağıya)
                for (rowIndex in 0 until rows) {
                    val date = LocalDate.fromEpochDays(
                        firstDateInColumn.toEpochDays() + rowIndex
                    )

                    when {
                        date > today -> {
                            // Gelecek tarihler için boş hücre
                            FutureDateCell(
                                boxSize = boxSize,
                                cornerRadius = cornerRadius
                            )
                        }
                        date >= earliestDate -> {
                            // Normal tarih hücresi
                            DateCell(
                                date = date,
                                progress = completedDates[date] ?: 0f,
                                isToday = date == today,
                                isFuture = false,
                                accentColor = accentColor,
                                boxSize = boxSize,
                                cornerRadius = cornerRadius,
                                onClick = { onDateClick(date) }
                            )
                        }
                        else -> {
                            // Tarih aralığı dışındaki hücreler için boşluk
                            Spacer(modifier = Modifier.size(boxSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    progress: Float,
    isToday: Boolean,
    isFuture: Boolean,
    accentColor: Color,
    boxSize: Dp,
    cornerRadius: Dp,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            progress >= 1f -> accentColor
            progress >= 0.75f -> accentColor.copy(alpha = 0.8f)
            progress >= 0.5f -> accentColor.copy(alpha = 0.6f)
            progress > 0f -> accentColor.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "bg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !isFuture) { onClick() }
    ) {
        when {
            isFuture -> {
                // Gelecek günler için hiçbir şey gösterme
            }
            progress >= 1f -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(boxSize * 0.5f),
                    tint = Color.White
                )
            }
            progress > 0f -> {
                Text(
                    text = "${(progress * 100).toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 0.5f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                // Tamamlanmamış günler için sadece arka plan rengi göster
                // İsteğe bağlı: Tarih numarasını gösterebilirsiniz
                /*Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )*/
            }
        }
    }
}

@Composable
private fun FutureDateCell(
    boxSize: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    )
}

private fun getMonthAbbreviation(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}