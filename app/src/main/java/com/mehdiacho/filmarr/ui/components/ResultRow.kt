package com.mehdiacho.filmarr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

/** The universal film row (§2.5): poster · title+meta · trailing add(＋)→✓. */
@Composable
fun ResultRow(
    film: Film,
    logged: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
    subtitle: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // poster 42×60
        Box(
            Modifier.size(width = 42.dp, height = 60.dp).clip(RoundedCornerShape(Dimens.rSm))
                .background(com.mehdiacho.filmarr.ui.theme.PosterGradients.forKey(film.gradientKey).brush)
        )
        Column(Modifier.weight(1f)) {
            Text(film.title, style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(2.dp))
            Text(
                subtitle ?: "${film.year} · ${film.genreLine}",
                style = FilmarrType.meta, color = FilmarrColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        AddButton(logged, onAdd)
    }
}

@Composable
private fun AddButton(logged: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (logged) 1.08f else 1f, label = "addPop")
    Box(
        Modifier.size(32.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(if (logged) FilmarrColors.Green else FilmarrColors.SurfaceCard)
            .then(if (logged) Modifier else Modifier.border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rFull)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (logged) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = if (logged) "Logged" else "Log watched",
            tint = if (logged) FilmarrColors.OnGreen else FilmarrColors.TextSecondary,
            modifier = Modifier.size(17.dp),
        )
    }
}
