package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.FilmarrToggle
import com.mehdiacho.filmarr.ui.components.ProgressTrack
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun LetterboxdImportScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.align(Alignment.Start).padding(top = 8.dp)) { BackCircle(onBack = onBack) }

        Spacer(Modifier.height(20.dp))
        // LBXD 3-dot mark
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Color(0xFF00E054)))
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Color(0xFF40BCF4)))
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Color(0xFFFF8000)))
        }
        Spacer(Modifier.height(16.dp))
        Text("Import from Letterboxd", style = FilmarrType.display.copy(fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Drop your Letterboxd export .zip and we'll match every film, rating, and review to your library.", style = FilmarrType.body, color = FilmarrColors.TextTertiary, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))
        // Progress card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.rLg)).background(FilmarrColors.SurfaceRaised)
                .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rLg)).padding(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Importing…", style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary)
                Text("862 / 1204", style = FilmarrType.labelStrong, color = FilmarrColors.TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            ProgressTrack(progress = 862f / 1204f, height = 8.dp, fill = Brush.horizontalGradient(listOf(Color(0xFF00E054), FilmarrColors.Green)))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("841", "Matched", FilmarrColors.Green)
                Stat("14", "To review", FilmarrColors.Gold)
                Stat("349", "Queued", FilmarrColors.TextTertiary)
            }
        }

        Eyebrow("Also sync", modifier = Modifier.align(Alignment.Start).padding(top = 24.dp, bottom = 12.dp))
        SyncToggle("Ratings")
        SyncToggle("Reviews")
        SyncToggle("Watchlist")
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = FilmarrType.statNumeral.copy(fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)), color = color)
        Text(label, style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
    }
}

@Composable
private fun SyncToggle(label: String) {
    var on by remember { mutableStateOf(true) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(Dimens.rMd))
            .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary, modifier = Modifier.weight(1f))
        FilmarrToggle(checked = on, onCheckedChange = { on = it })
    }
}
