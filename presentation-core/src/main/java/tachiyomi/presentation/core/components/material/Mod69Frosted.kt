package tachiyomi.presentation.core.components.material

import androidx.compose.runtime.staticCompositionLocalOf

// KMK -->
/**
 * 69 mod U1 / U4: frosted-glass look. 0 = off, 1 = light, 2 = strong.
 * Set by the app theme from the user's setting; read by the bars and the Scaffold.
 */
val LocalMod69Frosted = staticCompositionLocalOf { 0 }

/** How see-through the bars are with the frosted look on. */
fun mod69FrostedAlpha(level: Int): Float = when (level) {
    1 -> 0.72f
    2 -> 0.5f
    else -> 1f
}
// KMK <--
