package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.max

// KMK -->
/** 69 mod R1: page turn animations for the page-by-page reader. */
object Mod69PageTransformers {

    /** Returns (draw in reverse order, transformer) or null for the normal slide. */
    fun create(mode: String, horizontal: Boolean): Pair<Boolean, ViewPager.PageTransformer>? = when (mode) {
        "fade" -> false to ViewPager.PageTransformer { page, position ->
            reset(page)
            hold(page, position, horizontal)
            page.alpha = if (abs(position) >= 1f) 0f else 1f - abs(position)
        }
        "stack" -> true to ViewPager.PageTransformer { page, position ->
            reset(page)
            when {
                position <= -1f || position >= 1f -> page.alpha = 0f
                position <= 0f -> Unit
                else -> {
                    page.alpha = 1f - position
                    hold(page, position, horizontal)
                    val scale = 0.75f + 0.25f * (1f - position)
                    page.scaleX = scale
                    page.scaleY = scale
                }
            }
        }
        "cube" -> false to ViewPager.PageTransformer { page, position ->
            reset(page)
            if (abs(position) >= 1f) {
                page.alpha = 0f
            } else if (horizontal) {
                page.pivotX = if (position < 0f) page.width.toFloat() else 0f
                page.pivotY = page.height / 2f
                page.rotationY = 90f * position
            } else {
                page.pivotY = if (position < 0f) page.height.toFloat() else 0f
                page.pivotX = page.width / 2f
                page.rotationX = -90f * position
            }
        }
        "zoom" -> false to ViewPager.PageTransformer { page, position ->
            reset(page)
            if (abs(position) >= 1f) {
                page.alpha = 0f
            } else {
                val scale = max(0.85f, 1f - abs(position))
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.5f + (scale - 0.85f) / 0.15f * 0.5f
            }
        }
        "flip" -> false to ViewPager.PageTransformer { page, position ->
            reset(page)
            hold(page, position, horizontal)
            page.cameraDistance = page.width * 12f
            if (abs(position) >= 0.5f) {
                page.alpha = 0f
            } else if (horizontal) {
                page.rotationY = -180f * position
            } else {
                page.rotationX = 180f * position
            }
        }
        else -> null
    }

    /** Keeps the page where it is instead of sliding. */
    private fun hold(page: View, position: Float, horizontal: Boolean) {
        if (horizontal) page.translationX = -position * page.width else page.translationY = -position * page.height
    }

    private fun reset(page: View) {
        page.alpha = 1f
        page.translationX = 0f
        page.translationY = 0f
        page.scaleX = 1f
        page.scaleY = 1f
        page.rotationX = 0f
        page.rotationY = 0f
    }
}
// KMK <--
