@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mehdiacho.filmarr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mehdiacho.filmarr.R

/**
 * Type system — Ratio Spec §1.3.
 * Bricolage Grotesque = display / titles / numerals · Hanken Grotesk = all UI / body.
 * Both are bundled variable fonts; weights come from FontVariation (API 26+; graceful
 * fallback to the default instance on 24–25).
 */

private fun bricolage(weight: Int) = Font(
    R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun hanken(weight: Int) = Font(
    R.font.hanken_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Bricolage = FontFamily(
    bricolage(400), bricolage(500), bricolage(600), bricolage(700), bricolage(800),
)

val Hanken = FontFamily(
    hanken(400), hanken(500), hanken(600), hanken(700), hanken(800),
)

/** Named styles straight from §1.3 — screens use these for pixel-faithful text. */
object FilmarrType {
    val display = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.W800,
        fontSize = 32.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em,
    )
    val screenTitle = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.W800,
        fontSize = 26.sp, lineHeight = 26.sp, letterSpacing = (-0.02).em,
    )
    val posterTitle = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.W800,
        fontSize = 34.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em,
    )
    val cardTitle = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.W800,
        fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em,
    )
    val statNumeral = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.W800,
        fontSize = 26.sp, lineHeight = 26.sp,
    )
    val body = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W400,
        fontSize = 13.sp, lineHeight = 19.sp,
    )
    val labelStrong = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W700,
        fontSize = 14.sp, lineHeight = 16.sp,
    )
    val meta = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W500,
        fontSize = 12.sp, lineHeight = 17.sp,
    )
    val eyebrow = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W700,
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.10.em,
    )
    val navLabel = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W600,
        fontSize = 9.sp, lineHeight = 10.sp,
    )
    val badge = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.W800,
        fontSize = 14.sp, lineHeight = 14.sp, letterSpacing = 0.08.em,
    )
}

/** M3 Typography mapping so stock components inherit the brand faces. */
val FilmarrTypography = Typography(
    displayLarge = FilmarrType.display,
    headlineLarge = FilmarrType.screenTitle,
    headlineMedium = FilmarrType.cardTitle,
    titleLarge = FilmarrType.cardTitle,
    titleMedium = FilmarrType.labelStrong.copy(fontSize = 15.sp),
    bodyLarge = FilmarrType.body.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = FilmarrType.body,
    labelLarge = FilmarrType.labelStrong,
    labelMedium = FilmarrType.meta,
    labelSmall = FilmarrType.eyebrow,
)
