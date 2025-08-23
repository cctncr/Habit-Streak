package org.example.habit_streak.presention.ui.components.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.habit_streak.domain.model.HabitColor
import org.example.habit_streak.domain.model.HabitIcon
import org.example.habit_streak.domain.model.IconCategory
import org.example.habit_streak.presention.ui.components.common.IconItem
import org.example.habit_streak.presention.ui.theme.HabitStreakTheme

@Composable
fun IconSelectionGrid(
    selectedIcon: HabitIcon,
    selectedColor: HabitColor,
    onIconSelected: (HabitIcon) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(IconCategory.HEALTH) }

    Column(modifier = modifier) {
        // Category tabs
        ScrollableTabRow(
            selectedTabIndex = IconCategory.values().indexOf(selectedCategory),
            containerColor = Color.Transparent,
            contentColor = HabitStreakTheme.primaryTextColor,
            edgePadding = 0.dp
        ) {
            IconCategory.entries.forEach { category ->
                Tab(
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            text = category.displayName,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Icons grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                HabitIcon.values().filter { it.category == selectedCategory }
            ) { icon ->
                IconItem(
                    icon = icon,
                    isSelected = icon == selectedIcon,
                    color = HabitStreakTheme.habitColorToComposeColor(selectedColor),
                    onClick = { onIconSelected(icon) }
                )
            }
        }
    }
}
