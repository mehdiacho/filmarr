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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.data.tmdb.TmdbRepository
import com.mehdiacho.filmarr.ui.components.ConfirmButton
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.ResultRow
import com.mehdiacho.filmarr.ui.components.StarRow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenFilm: (String) -> Unit,
    onOpenAsk: () -> Unit,
    canLog: Boolean = true,
    onRequireAuth: () -> Unit = {},
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val library by FilmRepository.library.collectAsStateWithLifecycle()
    val loggedIds = library.filter { it.status == com.mehdiacho.filmarr.data.WatchStatus.WATCHED }.map { it.film.id }.toSet()
    var results by remember { mutableStateOf<List<Film>>(emptyList()) }
    LaunchedEffect(query.text) {
        val q = query.text.trim()
        if (q.isBlank()) {
            results = TmdbRepository.discover().ifEmpty { MockCatalog.films }
        } else {
            kotlinx.coroutines.delay(280) // debounce keystrokes
            val live = TmdbRepository.search(q)
            results = live.ifEmpty { MockCatalog.films.filter { it.title.contains(q, ignoreCase = true) } }
        }
    }
    var quickAdd by remember { mutableStateOf<Film?>(null) }

    Box(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "Search")
            // Search field
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).height(46.dp)
                    .clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = FilmarrColors.TextTertiary, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.text.isEmpty()) Text("Search films to log…", style = FilmarrType.body, color = FilmarrColors.TextTertiary)
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(FilmarrColors.Rose),
                        textStyle = FilmarrType.body.copy(color = FilmarrColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Eyebrow("Results") }
                items(results) { film ->
                    ResultRow(
                        film = film,
                        logged = film.id in loggedIds,
                        onClick = { onOpenFilm(film.id) },
                        onAdd = { if (canLog) FilmRepository.logWatched(film) else onRequireAuth() },
                        onLongPress = { if (canLog) quickAdd = film else onRequireAuth() },
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(Dimens.rMd))
                            .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
                            .clickable { onOpenAsk() }.padding(14.dp),
                    ) {
                        Text("Can't find it? Ask filmarr →", style = FilmarrType.labelStrong, color = FilmarrColors.Rose)
                    }
                }
            }
        }
        BackCircle(onBack = onBack, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 20.dp, top = 8.dp))

        quickAdd?.let { film ->
            QuickAddRadial(
                film = film,
                onDismiss = { quickAdd = null },
                onWatched = { FilmRepository.logWatched(film); quickAdd = null },
                onWatchlist = { FilmRepository.addToWatchlist(film); quickAdd = null },
                onRate = { FilmRepository.logWatched(film, it); quickAdd = null },
            )
        }
    }
}

/** Quick-add radial (§2.6) — signature one-press logging. */
@Composable
private fun QuickAddRadial(
    film: Film,
    onDismiss: () -> Unit,
    onWatched: () -> Unit,
    onWatchlist: () -> Unit,
    onRate: (Float) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(film.title, style = FilmarrType.cardTitle, color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
            ConfirmButton("Mark watched", onClick = onWatched, modifier = Modifier.fillMaxWidth().height(50.dp))
            Box(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rMd)).clickable(onClick = onWatchlist),
                contentAlignment = Alignment.Center,
            ) { Text("Add to watchlist", style = FilmarrType.labelStrong, color = FilmarrColors.Blue) }
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rMd)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Rate", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
                StarRow(rating = 0f, star = 24.dp, gap = 6.dp, onRate = onRate)
            }
        }
    }
}
