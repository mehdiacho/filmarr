package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

/** Screen header (§2.3): title + optional subtitle + trailing action circles. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = Dimens.base, end = Dimens.base, top = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = FilmarrType.meta, color = FilmarrColors.TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (actions != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = actions)
        }
    }
}

/** Translucent back circle used on detail/sub screens. */
@Composable
fun BackCircle(onBack: () -> Unit, modifier: Modifier = Modifier, translucent: Boolean = false) {
    Box(
        modifier.size(34.dp).clip(RoundedCornerShape(Dimens.rFull))
            .background(if (translucent) Color.Black.copy(alpha = 0.35f) else FilmarrColors.SurfaceCard)
            .then(if (translucent) Modifier else Modifier.border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rFull)))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
