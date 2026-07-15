package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.data.tmdb.TmdbRepository
import com.mehdiacho.filmarr.ui.components.ProgressTrack
import com.mehdiacho.filmarr.ui.components.SwipeDeck
import com.mehdiacho.filmarr.ui.components.rememberSwipeDeckState
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun BuildScreen(onOpenFilm: (String) -> Unit) {
    var films by remember { mutableStateOf<List<Film>>(emptyList()) }
    LaunchedEffect(Unit) {
        val live = TmdbRepository.buildFeed()
        films = live.ifEmpty { MockCatalog.buildFeed.map { it.film } }
    }
    val confidenceById = films.associate { it.id to "Likely seen · ${it.year}" }
    val deckState = rememberSwipeDeckState()
    val logged by FilmRepository.library.collectAsStateWithLifecycle()
    val loggedCount = 142 + logged.count { it.status == com.mehdiacho.filmarr.data.WatchStatus.WATCHED } - MockCatalog.library.count { it.status == com.mehdiacho.filmarr.data.WatchStatus.WATCHED }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        ScreenHeader(title = "Build", subtitle = "Films we think you've already seen")
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProgressTrack(
                progress = 0.38f, modifier = Modifier.weight(1f),
                fill = Brush.horizontalGradient(listOf(FilmarrColors.Green, FilmarrColors.Blue)),
            )
            Text("$loggedCount logged", style = FilmarrType.labelStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.TextSecondary)
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 78.dp)) {
            Text(
                "← NOT SEEN", style = FilmarrType.badge.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = FilmarrColors.Rose,
                modifier = Modifier.align(Alignment.CenterStart).rotate(-90f),
            )
            Text(
                "SEEN →", style = FilmarrType.badge.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = FilmarrColors.Green,
                modifier = Modifier.align(Alignment.CenterEnd).rotate(90f),
            )
            SwipeDeck(
                films = films,
                state = deckState,
                rightLabel = "SEEN",
                leftLabel = "NOT SEEN",
                onSwipeRight = { FilmRepository.logWatched(it) },
                onSwipeLeft = {},
                onTap = { onOpenFilm(it.id) },
                pill = { confidenceById[it.id] },
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            )
        }
    }
}
