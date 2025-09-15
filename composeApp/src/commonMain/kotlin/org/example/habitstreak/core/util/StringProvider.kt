package org.example.habitstreak.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun getLocalizedString(englishText: String, turkishText: String): String {
    // Use StateFlow directly instead of CompositionLocal for stronger recomposition trigger
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    val result = when (currentLocale) {
        AppLocale.ENGLISH -> englishText
        AppLocale.TURKISH -> turkishText
    }
    // Debug for locale issues
    println("🔤 getLocalizedString: Locale=${currentLocale.code}, EN='$englishText', TR='$turkishText', Result='$result'")
    return result
}

object Strings {
    @Composable fun nav_settings() = getLocalizedString("Settings", "Ayarlar")
    @Composable fun nav_back() = getLocalizedString("Back", "Geri")
    @Composable fun nav_statistics() = getLocalizedString("Statistics", "İstatistikler")
    @Composable fun nav_habits() = getLocalizedString("Habits", "Alışkanlıklar")

    @Composable fun section_general() = getLocalizedString("General", "Genel")
    @Composable fun section_notifications() = getLocalizedString("Notifications", "Bildirimler")
    @Composable fun section_data_privacy() = getLocalizedString("Data & Privacy", "Veri ve Gizlilik")
    @Composable fun section_about() = getLocalizedString("About", "Hakkında")

    @Composable fun settings_theme() = getLocalizedString("Theme", "Tema")
    @Composable fun settings_language() = getLocalizedString("Language", "Dil")
    @Composable fun settings_about() = getLocalizedString("About", "Hakkında")

    @Composable fun theme_system() = getLocalizedString("System default", "Sistem varsayılanı")
    @Composable fun theme_light() = getLocalizedString("Light", "Açık")
    @Composable fun theme_dark() = getLocalizedString("Dark", "Koyu")

    @Composable fun setting_archived_habits() = getLocalizedString("Archived Habits", "Arşivlenmiş Alışkanlıklar")
    @Composable fun setting_archived_habits_desc() = getLocalizedString("View and restore archived habits", "Arşivlenmiş alışkanlıkları görüntüle ve geri yükle")

    @Composable fun action_cancel() = getLocalizedString("Cancel", "İptal")
    @Composable fun action_restore() = getLocalizedString("Restore", "Geri Yükle")
}