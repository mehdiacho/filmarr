package com.mehdiacho.filmarr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import kotlinx.coroutines.delay

/** Undo snackbar bound to [FilmRepository] (§2.9). Auto-dismisses after 4s. */
@Composable
fun UndoSnackbarHost(modifier: Modifier = Modifier) {
    val event by FilmRepository.snackbar.collectAsStateWithLifecycle()

    LaunchedEffect(event?.id) {
        if (event != null) { delay(4000); FilmRepository.consumeSnackbar() }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().height(40.dp)
                .clip(RoundedCornerShape(Dimens.rMd))
                .background(FilmarrColors.SurfaceLifted)
                .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(event?.message ?: "", style = FilmarrType.meta, color = FilmarrColors.TextSecondary)
            Text(
                "Undo",
                style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = FilmarrColors.Rose,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                    FilmRepository.undo(); FilmRepository.consumeSnackbar()
                }.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}
