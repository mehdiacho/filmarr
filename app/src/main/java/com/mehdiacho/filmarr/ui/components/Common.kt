package com.mehdiacho.filmarr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.tmdb.backdropUrl
import com.mehdiacho.filmarr.data.tmdb.posterUrl
import com.mehdiacho.filmarr.ui.theme.PosterGradients
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

/** The brand dot-grid texture (§1.2). Drawn behind content, repeats every [stepDp]. */
fun Modifier.dotGrid(
    color: Color = Color.White.copy(alpha = 0.07f),
    stepDp: Dp = 4.dp,
    radiusDp: Dp = 0.6.dp,
): Modifier = drawBehind {
    val step = stepDp.toPx()
    val r = radiusDp.toPx()
    if (step <= 0f) return@drawBehind
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color, radius = r, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/** Ambient screen background — dot-grid over base (§1.2 utility surfaces). */
fun Modifier.ambientDotGrid(): Modifier =
    background(FilmarrColors.Bg).dotGrid(color = Color.White.copy(alpha = 0.045f), stepDp = 5.dp)

/**
 * Poster art surface (§2.4 layers): gradient (or later a real TMDB poster) + top highlight +
 * dot texture + bottom scrim. [ghostTitle] draws the big uppercase title ghost at the top.
 */
@Composable
fun PosterSurface(
    film: Film,
    modifier: Modifier = Modifier,
    ghostTitle: Boolean = true,
    scrimFraction: Float = 0.62f,
) {
    val grad = PosterGradients.forKey(film.gradientKey)
    val art = film.posterUrl("w780") ?: film.backdropUrl("w780")
    Box(modifier.background(grad.brush)) {
        if (art != null) {
            // Real TMDB art over the gradient fallback.
            AsyncImage(
                model = art,
                contentDescription = film.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Gradient-card treatment: top highlight + dot texture.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(120f, 70f),
                        radius = 520f,
                    )
                )
            )
            Box(Modifier.fillMaxSize().dotGrid())
        }
        // bottom scrim
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    (1f - scrimFraction) to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.92f),
                )
            )
        )
        if (ghostTitle && art == null) {
            Text(
                film.title.uppercase(),
                style = FilmarrType.posterTitle,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 20.dp, end = 18.dp),
            )
        }
    }
}

/** Uppercase tracked section label (§1.3 eyebrow). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = FilmarrColors.TextMuted) {
    Text(text.uppercase(), style = FilmarrType.eyebrow, color = color, modifier = modifier)
}

/** Small trailing action circle in headers (§2.3). */
@Composable
fun ActionCircle(modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(34.dp)
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(FilmarrColors.SurfaceCard)
            .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rFull))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Gold star row with half-star support (§ rating). Read-only unless [onRate] is set. */
@Composable
fun StarRow(
    rating: Float,
    modifier: Modifier = Modifier,
    star: Dp = 16.dp,
    tint: Color = FilmarrColors.Gold,
    gap: Dp = 2.dp,
    onRate: ((Float) -> Unit)? = null,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (i in 1..5) {
            val icon = when {
                rating >= i -> Icons.Filled.Star
                rating >= i - 0.5f -> Icons.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            val filled = rating >= i - 0.5f
            Icon(
                icon, contentDescription = null,
                tint = if (filled) tint else FilmarrColors.TextMuted,
                modifier = Modifier.size(star).let {
                    if (onRate != null) it.clickable { onRate(i.toFloat()) } else it
                },
            )
        }
    }
}

/** Thin progress track (§2.8). */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    fill: Brush = Brush.horizontalGradient(listOf(FilmarrColors.Rose, FilmarrColors.Rose)),
    track: Color = Color.White.copy(alpha = 0.09f),
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(Dimens.rFull))
                .background(fill)
        )
    }
}

/** Centered helper for a big directional swipe badge (§2.4). */
@Composable
fun DirectionalBadge(text: String, color: Color, rotation: Float, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha; rotationZ = rotation }
            .clip(RoundedCornerShape(9.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(2.5.dp, color, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = FilmarrType.badge, color = color)
    }
}

@Composable
fun CenteredCaption(text: String, modifier: Modifier = Modifier, color: Color = FilmarrColors.TextTertiary) {
    Text(text, style = FilmarrType.meta, color = color, textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth())
}

/** Simple metadata row: year · genres · ★rating. */
@Composable
fun MetaRow(film: Film, modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.75f)) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("${film.year}", style = FilmarrType.meta, color = color)
        Dot()
        Text(film.genreLine, style = FilmarrType.meta, color = color)
        Dot()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Star, null, tint = FilmarrColors.Gold, modifier = Modifier.size(12.dp))
            Text(String.format("%.1f", film.rating), style = FilmarrType.meta, color = color)
        }
    }
}

@Composable
private fun Dot() {
    Text("·", style = FilmarrType.meta, color = Color.White.copy(alpha = 0.4f))
}
