package com.mehdiacho.filmarr.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Filmarr palette — the single (dark) scheme from the Ratio Spec §1.1.
 * Color = meaning: rose commits, green = watched/interested, blue = watchlist/watching,
 * gold = rating, violet = graph.
 */
object FilmarrColors {
    val Bg = Color(0xFF0A0A0A)            // surface.base — app bg, nav bar, sheets base
    val SurfaceRaised = Color(0xFF0F1014) // stat cards, list containers
    val SurfaceCard = Color(0xFF161616)   // chips, search field, secondary buttons, rows
    val SurfaceLifted = Color(0xFF1C1C1C) // pressed/lifted card, snackbar
    val SheetBg = Color(0xFF141519)       // bottom sheets (confidence log)

    val Rose = Color(0xFFF43F5E)          // brand, primary CTA, skip, heart, FAB, active nav
    val Gold = Color(0xFFEAB308)          // ratings/stars, "maybe", review flag
    val Blue = Color(0xFF3B82F6)          // watching, watchlist, AI user bubble
    val Green = Color(0xFF22C55E)         // interested/watched, confirm, add
    val Violet = Color(0xFFD946EF)        // graph node, poster gradient

    val OnGreen = Color(0xFF06250F)       // text on green confirm buttons

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFCBD0D6)
    val TextTertiary = Color(0xFF8E8E93)
    val TextMuted = Color(0xFF5C5F66)

    val Hairline = Color.White.copy(alpha = 0.08f)
    val HairlineStrong = Color.White.copy(alpha = 0.12f)

    fun hairline(alpha: Float) = Color.White.copy(alpha = alpha)
}

/** Placeholder poster gradients (Ratio Spec §1.2). Real TMDB posters replace these. */
data class PosterGradient(val start: Color, val end: Color) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
}

object PosterGradients {
    val NeonHarbor = PosterGradient(Color(0xFFF43F5E), Color(0xFF7C1D3A))
    val Saltwater = PosterGradient(Color(0xFF3B82F6), Color(0xFF1E1B4B))
    val EmberAsh = PosterGradient(Color(0xFFF59E0B), Color(0xFF7C2D12))
    val VelvetStatic = PosterGradient(Color(0xFFD946EF), Color(0xFF2E1065))
    val QuietYear = PosterGradient(Color(0xFF0EA5A4), Color(0xFF0A1F22))
    val LowTide = PosterGradient(Color(0xFF22C55E), Color(0xFF052E16))

    private val all = listOf(NeonHarbor, Saltwater, EmberAsh, VelvetStatic, QuietYear, LowTide)

    /** Deterministic gradient for a film without real art, keyed by a stable id/title. */
    fun forKey(key: String): PosterGradient = all[abs(key.hashCode()) % all.size]
}
