package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.ResultRow
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TasteGraphScreen(onBack: () -> Unit, onOpenFilm: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
            Column {
                Text("Your taste graph", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
                Text("Why we recommend what we do", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
            }
        }

        // Graph canvas
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val nodeColors = listOf(FilmarrColors.Violet, FilmarrColors.Blue, FilmarrColors.Green, FilmarrColors.Gold, FilmarrColors.Rose)
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension * 0.33f
                val n = MockCatalog.tasteGraph.size
                val satellites = List(n) { i ->
                    val a = (i.toFloat() / n) * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
                    Offset(center.x + radius * cos(a), center.y + radius * sin(a))
                }
                satellites.forEachIndexed { i, p ->
                    drawLine(nodeColors[i % nodeColors.size].copy(alpha = 0.4f), center, p, strokeWidth = 2f)
                }
                satellites.forEachIndexed { i, p ->
                    drawCircle(nodeColors[i % nodeColors.size], radius = 8.dp.toPx(), center = p)
                }
                drawCircle(
                    brush = Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239))),
                    radius = 11.dp.toPx(), center = center,
                )
            }
            Text("YOU", style = FilmarrType.eyebrow, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(top = 26.dp))
        }

        Eyebrow("Recommended from your graph", modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Column(Modifier.padding(bottom = 90.dp)) {
            ResultRow(
                film = MockCatalog.byId("neon-harbor"), logged = false,
                onClick = { onOpenFilm("neon-harbor") }, onAdd = { FilmRepository.addToWatchlist(MockCatalog.byId("neon-harbor")) },
                subtitle = "92% match · via A24 core",
            )
        }
    }
}
