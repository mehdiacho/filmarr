package com.mehdiacho.filmarr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Fixed density tokens — Ratio Spec §1.4 (spacing) and §1.5 (radius). Never scaled by width. */
object Dimens {
    // Spacing
    val xs = 6.dp
    val sm = 9.dp
    val md = 12.dp
    val md14 = 14.dp
    val base = 20.dp   // screen edge margin (width-class min 16 / max 28 — see §1.6)
    val lg = 22.dp
    val xl = 44.dp

    // Corner radius
    val rSm = 8.dp
    val rMd = 14.dp
    val rLg = 16.dp
    val rXl = 24.dp
    val rFull = 999.dp

    val ShapeSm = RoundedCornerShape(8.dp)
    val ShapeMd = RoundedCornerShape(14.dp)
    val ShapeLg = RoundedCornerShape(16.dp)
    val ShapeXl = RoundedCornerShape(24.dp)
    val ShapeFull = RoundedCornerShape(999.dp)
}
