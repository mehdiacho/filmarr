package com.mehdiacho.filmarr.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

/** Primary CTA — 52dp rose pill (§2.7). */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FilmarrColors.Rose,
    textColor: Color = Color.White,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(Dimens.rLg))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content?.invoke(this)
        Text(text, style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)), color = textColor)
    }
}

/** Green confirm button — 44dp (§2.7). */
@Composable
fun ConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(FilmarrColors.Green)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content?.invoke(this)
        Text(text, style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.OnGreen)
    }
}

/** 46×44 secondary icon button (§2.7). */
@Composable
fun IconSquareButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(width = 46.dp, height = 44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(FilmarrColors.SurfaceCard)
            .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Genre chip — pill, selected = rose (§2.7). */
@Composable
fun GenreChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) FilmarrColors.Rose else FilmarrColors.SurfaceCard, label = "chipBg")
    Box(
        modifier
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(bg)
            .then(if (selected) Modifier else Modifier.border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rFull)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            if (selected) "$text ✓" else text,
            style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = if (selected) Color.White else FilmarrColors.TextSecondary,
        )
    }
}

/** In-place segmented control (§2.7). */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(FilmarrColors.SurfaceCard)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.rFull))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = FilmarrType.meta.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = if (active) FilmarrColors.Bg else FilmarrColors.TextTertiary,
                )
            }
        }
    }
}

/** Track toggle 42×25 (§2.7). */
@Composable
fun FilmarrToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val align by animateFloatAsState(if (checked) 1f else 0f, label = "toggle")
    Box(
        modifier
            .size(width = 42.dp, height = 25.dp)
            .clip(RoundedCornerShape(Dimens.rFull))
            .background(if (checked) FilmarrColors.Green else Color(0xFF2A2A2A))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = (align * 17).dp)
                .size(19.dp)
                .clip(RoundedCornerShape(Dimens.rFull))
                .background(Color.White)
        )
    }
}
