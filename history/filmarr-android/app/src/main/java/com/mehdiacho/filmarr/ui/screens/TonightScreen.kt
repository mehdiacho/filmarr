package com.mehdiacho.filmarr.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.PosterSurface
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.components.SegmentedControl
import com.mehdiacho.filmarr.ui.components.ambientDotGrid
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun TonightScreen(onBack: () -> Unit, onOpenFilm: (String) -> Unit) {
    var mood by remember { mutableIntStateOf(0) }
    var time by remember { mutableFloatStateOf(120f) }
    var withWho by remember { mutableIntStateOf(0) }
    var pickIndex by remember { mutableIntStateOf(0) }
    val pick = MockCatalog.discoverDeck[pickIndex % MockCatalog.discoverDeck.size]

    Column(
        Modifier.fillMaxSize().ambientDotGrid().statusBarsPadding().padding(horizontal = 22.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
        }
        Text("What's the mood\ntonight?", style = FilmarrType.display, color = FilmarrColors.TextPrimary, modifier = Modifier.padding(top = 16.dp))

        Eyebrow("Mood", modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MockCatalog.moods.forEachIndexed { i, m ->
                val sel = i == mood
                Box(
                    Modifier.clip(RoundedCornerShape(Dimens.rFull))
                        .background(if (sel) FilmarrColors.Rose else FilmarrColors.SurfaceCard)
                        .then(if (sel) Modifier else Modifier.border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rFull)))
                        .clickable { mood = i }.padding(horizontal = 14.dp, vertical = 9.dp),
                ) { Text(m, style = FilmarrType.meta, color = if (sel) Color.White else FilmarrColors.TextSecondary) }
            }
        }

        Eyebrow("Time you've got — ${time.toInt()} min", modifier = Modifier.padding(top = 22.dp, bottom = 4.dp))
        Slider(
            value = time, onValueChange = { time = it }, valueRange = 60f..240f,
            colors = SliderDefaults.colors(thumbColor = FilmarrColors.Rose, activeTrackColor = FilmarrColors.Rose, inactiveTrackColor = FilmarrColors.SurfaceCard),
        )

        Eyebrow("Watching with", modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
        SegmentedControl(listOf("Solo", "Date", "Group"), withWho, { withWho = it })

        Box(Modifier.weight(1f))

        // The pick
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(Dimens.rXl)).clickable { onOpenFilm(pick.id) },
        ) {
            PosterSurface(pick, Modifier.fillMaxSize(), ghostTitle = false)
            Box(
                Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(Dimens.rFull))
                    .background(FilmarrColors.Rose).padding(horizontal = 10.dp, vertical = 5.dp),
            ) { Text("YOUR PICK · 94% match", style = FilmarrType.eyebrow, color = Color.White) }
            Text(pick.title, style = FilmarrType.cardTitle, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryCta("Watch this", onClick = { FilmRepository.markWatching(pick); onOpenFilm(pick.id) }, modifier = Modifier.weight(1f))
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(Dimens.rLg)).background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rLg)).clickable { pickIndex++ },
                contentAlignment = Alignment.Center,
            ) { Text("Reroll", style = FilmarrType.labelStrong, color = FilmarrColors.TextSecondary) }
        }
    }
}
