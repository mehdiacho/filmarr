package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import com.mehdiacho.filmarr.ui.theme.PosterGradients

@Composable
fun BlitzScreen(onBack: () -> Unit) {
    val tiles = remember { MockCatalog.blitz }
    val logged = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackCircle(onBack = onBack)
                Column {
                    Text("Backlog Blitz", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
                    Text("Log fast. Build your history.", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
                }
            }
            Text("${logged.size} today", style = FilmarrType.labelStrong, color = FilmarrColors.Green)
        }
        // Streak chip
        Row(
            Modifier.padding(start = 20.dp, top = 14.dp).clip(RoundedCornerShape(Dimens.rFull))
                .background(FilmarrColors.Rose.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = FilmarrColors.Rose, modifier = Modifier.size(16.dp))
            Text("6-day streak", style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.Rose)
        }

        Eyebrow("Popular — you likely saw these", modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
        ) {
            items(tiles) { film ->
                val isLogged = film.id in logged
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(Dimens.rMd))
                        .background(PosterGradients.forKey(film.gradientKey).brush)
                        .clickable {
                            if (isLogged) logged.remove(film.id) else { logged.add(film.id); FilmRepository.logWatched(film) }
                        },
                ) {
                    if (isLogged) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(RoundedCornerShape(Dimens.rFull)).background(FilmarrColors.Green),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Check, null, tint = FilmarrColors.OnGreen, modifier = Modifier.size(15.dp)) }
                    }
                    Text(
                        film.title, style = FilmarrType.labelStrong, color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    )
                }
            }
        }
    }
}
