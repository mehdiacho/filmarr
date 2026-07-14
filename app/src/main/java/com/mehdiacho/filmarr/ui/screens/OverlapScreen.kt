package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.components.ResultRow
import com.mehdiacho.filmarr.ui.components.ambientDotGrid
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun OverlapScreen(onBack: () -> Unit, onOpenFilm: (String) -> Unit) {
    Column(Modifier.fillMaxSize().ambientDotGrid().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
            // overlapping avatars
            Box(Modifier.size(width = 58.dp, height = 34.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239)))))
                Box(Modifier.offset(x = 24.dp).size(34.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Brush.linearGradient(listOf(FilmarrColors.Blue, Color(0xFF1E1B4B)))))
            }
            Column {
                Text("You × Ana", style = FilmarrType.cardTitle, color = FilmarrColors.TextPrimary)
                Text("Taste overlap · 68%", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
            }
        }

        // Venn
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = 88.dp.toPx()
                val cy = size.height / 2
                val left = Offset(size.width / 2 - r * 0.55f, cy)
                val right = Offset(size.width / 2 + r * 0.55f, cy)
                drawCircle(FilmarrColors.Rose.copy(alpha = 0.13f), r, left)
                drawCircle(FilmarrColors.Blue.copy(alpha = 0.13f), r, right)
                drawCircle(FilmarrColors.Rose.copy(alpha = 0.5f), r, left, style = Stroke(2f))
                drawCircle(FilmarrColors.Blue.copy(alpha = 0.5f), r, right, style = Stroke(2f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow("Yours", color = FilmarrColors.Rose)
                Eyebrow("The overlap", color = FilmarrColors.TextSecondary)
                Eyebrow("Ana's", color = FilmarrColors.Blue)
            }
        }

        Eyebrow("In the overlap — neither of you has seen", modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(MockCatalog.byId("north-signal"), MockCatalog.byId("paper-moths")).forEach { film ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultRow(film = film, logged = false, onClick = { onOpenFilm(film.id) }, onAdd = { FilmRepository.addToWatchlist(film) }, subtitle = "Both-match · 88%", modifier = Modifier.weight(1f))
                }
            }
        }
        Box(Modifier.weight(1f))
        PrimaryCta("Watch together", onClick = {}, modifier = Modifier.padding(bottom = 90.dp))
    }
}
