package org.example.habitstreak.presentation.ui.utils

import androidx.compose.foundation.layout.navigationBarsPadding as androidNavigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.navigationBarsPadding(): Modifier = this.androidNavigationBarsPadding()