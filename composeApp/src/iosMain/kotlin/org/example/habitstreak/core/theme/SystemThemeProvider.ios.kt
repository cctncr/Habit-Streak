package org.example.habitstreak.core.theme

import platform.UIKit.UIScreen
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle

actual object SystemThemeProvider {
    actual fun isSystemInDarkMode(): Boolean {
        return UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}