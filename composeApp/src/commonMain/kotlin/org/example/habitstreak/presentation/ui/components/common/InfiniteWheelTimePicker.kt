package org.example.habitstreak.presentation.ui.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Enhanced infinite wheel time picker with improved visuals
 */
@Composable
fun InfiniteWheelTimePicker(
    selectedTime: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = false
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .height(240.dp)
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour wheel
        InfiniteWheelColumn(
            items = if (is24Hour) (0..23).toList() else (1..12).toList(),
            selectedIndex = if (is24Hour) selectedTime.hour else {
                // Convert 24-hour to 12-hour display (1-12)
                val displayHour = when (selectedTime.hour) {
                    0 -> 12  // 12 AM
                    in 1..12 -> selectedTime.hour  // 1 AM - 12 PM
                    else -> selectedTime.hour - 12  // 1 PM - 11 PM
                }
                displayHour - 1  // Convert to 0-based index
            },
            onSelectionChanged = { hourIndex ->
                val newHour = if (is24Hour) {
                    hourIndex
                } else {
                    // Convert from 0-based index to 1-12 display hour
                    val displayHour = hourIndex + 1
                    // Convert to 24-hour format
                    when {
                        selectedTime.hour < 12 -> { // Currently AM
                            if (displayHour == 12) 0 else displayHour // 12 AM = 0, others stay same
                        }
                        else -> { // Currently PM
                            if (displayHour == 12) 12 else displayHour + 12 // 12 PM = 12, others +12
                        }
                    }
                }
                onTimeChanged(LocalTime(newHour, selectedTime.minute))
            },
            itemHeight = 48.dp,
            visibleItemCount = 5,
            modifier = Modifier.weight(1f)
        ) { item ->
            if (is24Hour) "%02d".format(item) else item.toString()
        }

        // Separator
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = primaryColor,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Minute wheel
        InfiniteWheelColumn(
            items = (0..59).toList(),
            selectedIndex = selectedTime.minute,
            onSelectionChanged = { minute ->
                onTimeChanged(LocalTime(selectedTime.hour, minute))
            },
            itemHeight = 48.dp,
            visibleItemCount = 5,
            modifier = Modifier.weight(1f)
        ) { item ->
            "%02d".format(item)
        }

        // AM/PM selector (non-infinite for 12-hour format)
        if (!is24Hour) {
            Spacer(modifier = Modifier.width(16.dp))

            SimpleWheelColumn(
                items = listOf("AM", "PM"),
                selectedIndex = if (selectedTime.hour < 12) 0 else 1,
                onSelectionChanged = { amPmIndex ->
                    val currentDisplayHour = when (selectedTime.hour) {
                        0 -> 12
                        in 1..12 -> selectedTime.hour
                        else -> selectedTime.hour - 12
                    }

                    val newHour = if (amPmIndex == 0) { // AM
                        if (currentDisplayHour == 12) 0 else currentDisplayHour
                    } else { // PM
                        if (currentDisplayHour == 12) 12 else currentDisplayHour + 12
                    }

                    onTimeChanged(LocalTime(newHour, selectedTime.minute))
                },
                itemHeight = 48.dp,
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

/**
 * Infinite scrolling wheel column for hours and minutes
 */
@Composable
private fun <T> InfiniteWheelColumn(
    items: List<T>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    itemHeight: Dp,
    visibleItemCount: Int,
    modifier: Modifier = Modifier,
    itemText: (T) -> String
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val halfVisibleItems = visibleItemCount / 2
    val coroutineScope = rememberCoroutineScope()

    // Create infinite list
    val infiniteItems = remember(items) {
        val repetitions = 10000
        List(repetitions * items.size) { index ->
            items[index % items.size]
        }
    }

    val startIndex = remember(items, selectedIndex) {
        val middleRepetition = 5000
        middleRepetition * items.size + selectedIndex
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex
    )

    // Track scroll offset for smooth animations
    var scrollOffset by remember { mutableStateOf(0f) }

    // Track when scrolling stops to trigger snap
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()

        // Calculate center item - the content padding already centers the list
        val scrollOffsetItems = listState.firstVisibleItemScrollOffset / itemHeightPx
        val exactCenterIndex = listState.firstVisibleItemIndex + scrollOffsetItems

        // Round to nearest for selection
        val centerItemIndex = exactCenterIndex.roundToInt()
        val actualIndex = ((centerItemIndex % items.size) + items.size) % items.size

        if (actualIndex != selectedIndex) {
            onSelectionChanged(actualIndex)
        }
    }

    // Detect when scrolling stops and snap to nearest item
    LaunchedEffect(listState.isScrollInProgress) {
        if (isScrolling && !listState.isScrollInProgress) {
            // Scrolling just stopped, snap to nearest item
            val scrollOffsetItems = listState.firstVisibleItemScrollOffset / itemHeightPx
            val exactCenterIndex = listState.firstVisibleItemIndex + scrollOffsetItems
            val targetIndex = exactCenterIndex.roundToInt()

            listState.animateScrollToItem(targetIndex)
        }
        isScrolling = listState.isScrollInProgress
    }

    Box(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * halfVisibleItems),
            verticalArrangement = Arrangement.Center
        ) {
            items(infiniteItems.size) { index ->
                val item = infiniteItems[index]

                // Calculate distance from center using exact same logic as selection
                val itemPosition = index.toFloat()
                val scrollOffsetItems = listState.firstVisibleItemScrollOffset / itemHeightPx
                val exactCenterPosition = listState.firstVisibleItemIndex + scrollOffsetItems
                val distanceFromCenter = abs(itemPosition - exactCenterPosition)

                // More precise scale calculation for center item
                val scale = when {
                    distanceFromCenter < 0.3f -> 1.4f // Clearly larger center item
                    distanceFromCenter < 1.0f -> 1.4f - ((distanceFromCenter - 0.3f) * 0.6f) // Smooth transition
                    distanceFromCenter < 2.0f -> 1.0f - ((distanceFromCenter - 1.0f) * 0.2f)
                    else -> 0.8f
                }

                // More precise alpha calculation
                val alpha = when {
                    distanceFromCenter < 0.3f -> 1.0f // Full opacity for center
                    distanceFromCenter < 1.0f -> 1.0f - ((distanceFromCenter - 0.3f) * 0.2f)
                    distanceFromCenter < 2.0f -> 0.8f - ((distanceFromCenter - 1.0f) * 0.4f)
                    else -> 0.3f
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .scale(scale)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemText(item),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (distanceFromCenter < 0.3f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Center selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .padding(horizontal = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // Gradient overlays
        Column(modifier = Modifier.fillMaxSize()) {
            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * halfVisibleItems)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = with(density) { (itemHeight * halfVisibleItems).toPx() }
                        )
                    )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * halfVisibleItems)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            ),
                            startY = 0f,
                            endY = with(density) { (itemHeight * halfVisibleItems).toPx() }
                        )
                    )
            )
        }
    }
}

/**
 * Simple wheel column for AM/PM selection (non-infinite)
 */
@Composable
private fun SimpleWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )

    // Track scroll offset for smooth animations
    var scrollOffset by remember { mutableStateOf(0f) }
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
    }

    // Detect when scrolling stops and snap to nearest item
    LaunchedEffect(listState.isScrollInProgress) {
        if (isScrolling && !listState.isScrollInProgress) {
            // Scrolling just stopped, snap to nearest item
            val scrollOffsetItems = listState.firstVisibleItemScrollOffset / itemHeightPx
            val exactCenterIndex = listState.firstVisibleItemIndex + scrollOffsetItems
            val targetIndex = exactCenterIndex.roundToInt().coerceIn(0, items.size - 1)

            listState.animateScrollToItem(targetIndex)
            onSelectionChanged(targetIndex)
        }
        isScrolling = listState.isScrollInProgress
    }

    Box(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            verticalArrangement = Arrangement.Center,
            userScrollEnabled = true
        ) {
            items(items.size) { index ->
                // Calculate distance from center using same logic as infinite wheel
                val itemPosition = index.toFloat()
                val scrollOffsetItems = listState.firstVisibleItemScrollOffset / itemHeightPx
                val exactCenterPosition = listState.firstVisibleItemIndex + scrollOffsetItems
                val distanceFromCenter = abs(itemPosition - exactCenterPosition)

                // More precise scale calculation for center item
                val scale = when {
                    distanceFromCenter < 0.3f -> 1.4f // Clearly larger center item
                    distanceFromCenter < 1.0f -> 1.4f - ((distanceFromCenter - 0.3f) * 0.6f)
                    distanceFromCenter < 2.0f -> 1.0f - ((distanceFromCenter - 1.0f) * 0.2f)
                    else -> 0.8f
                }

                // More precise alpha calculation
                val alpha = when {
                    distanceFromCenter < 0.3f -> 1.0f // Full opacity for center
                    distanceFromCenter < 1.0f -> 1.0f - ((distanceFromCenter - 0.3f) * 0.2f)
                    distanceFromCenter < 2.0f -> 0.8f - ((distanceFromCenter - 1.0f) * 0.4f)
                    else -> 0.3f
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .scale(scale)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (distanceFromCenter < 0.3f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Center selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .padding(horizontal = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // Gradient overlays (lighter for AM/PM)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * 2)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * 2)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }
    }

    // Snap to selected index when selection changes
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }
}