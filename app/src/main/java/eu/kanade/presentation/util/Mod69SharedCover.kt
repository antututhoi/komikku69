package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import eu.kanade.domain.ui.UiPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod U11: the cover grows from the library into the entry page. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalMod69SharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalMod69AnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

object Mod69SharedCover {
    val enabled: Boolean by lazy { Injekt.get<UiPreferences>().mod69CoverGrow().get() }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.mod69SharedCover(mangaId: Long): Modifier {
    if (!Mod69SharedCover.enabled || mangaId <= 0) return this
    val shared = LocalMod69SharedScope.current ?: return this
    val animated = LocalMod69AnimatedScope.current ?: return this
    return with(shared) {
        val state = rememberSharedContentState(key = "mod69-cover-$mangaId")
        this@mod69SharedCover.sharedElement(
            sharedContentState = state,
            animatedVisibilityScope = animated,
        )
    }
}
// KMK <--
