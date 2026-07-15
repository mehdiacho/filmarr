package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.LibraryEntry
import com.mehdiacho.filmarr.data.WatchStatus
import com.mehdiacho.filmarr.data.tmdb.posterUrl
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.SegmentedControl
import com.mehdiacho.filmarr.ui.components.StarRow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import com.mehdiacho.filmarr.ui.theme.PosterGradients

@Composable
fun LibraryScreen(onOpenFilm: (String) -> Unit) {
    val library by FilmRepository.library.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val watched = library.filter { it.status == WatchStatus.WATCHED }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        ScreenHeader(title = "Library", subtitle = "${watched.size} films logged")
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            SegmentedControl(listOf("Grid", "Diary", "Lists"), tab, { tab = it })
        }
        when (tab) {
            0 -> GridView(watched, onOpenFilm)
            1 -> DiaryView(watched, onOpenFilm)
            else -> ListsView(library, onOpenFilm)
        }
    }
}

@Composable
private fun GridView(entries: List<LibraryEntry>, onOpenFilm: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
    ) {
        items(entries) { e ->
            Box(
                Modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(Dimens.rSm))
                    .background(PosterGradients.forKey(e.film.gradientKey).brush)
                    .clickable { onOpenFilm(e.film.id) }
            ) {
                e.film.posterUrl("w342")?.let { url ->
                    AsyncImage(url, e.film.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                if (e.userRating != null) {
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(6.dp)
                            .clip(RoundedCornerShape(Dimens.rFull)).background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Star, null, tint = FilmarrColors.Gold, modifier = Modifier.size(10.dp))
                        Text(" ${e.userRating}", style = FilmarrType.meta.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryView(entries: List<LibraryEntry>, onOpenFilm: (String) -> Unit) {
    val byMonth = entries.filter { it.monthLabel != null }.groupBy { it.monthLabel!! }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
    ) {
        byMonth.forEach { (month, list) ->
            item { Eyebrow(month, modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)) }
            items(list) { e -> DiaryRow(e, onOpenFilm) }
        }
    }
}

@Composable
private fun DiaryRow(e: LibraryEntry, onOpenFilm: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).clickable { onOpenFilm(e.film.id) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(e.loggedLabel ?: "", style = FilmarrType.statNumeral.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.TextMuted, modifier = Modifier.size(width = 30.dp, height = 30.dp))
        Box(Modifier.size(width = 38.dp, height = 54.dp).clip(RoundedCornerShape(Dimens.rSm)).background(PosterGradients.forKey(e.film.gradientKey).brush)) {
            e.film.posterUrl("w185")?.let { url ->
                AsyncImage(url, e.film.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f)) {
            Text(e.film.title, style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary)
            StarRow(rating = e.userRating ?: 0f, star = 12.dp, modifier = Modifier.padding(vertical = 3.dp))
            if (e.review != null) {
                Text(e.review, style = FilmarrType.meta, color = FilmarrColors.TextTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ListsView(library: List<LibraryEntry>, onOpenFilm: (String) -> Unit) {
    val watchlist = library.filter { it.status == WatchStatus.WATCHLIST }
    val watching = library.filter { it.status == WatchStatus.WATCHING }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
    ) {
        if (watching.isNotEmpty()) {
            item { Eyebrow("Currently watching", modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)) }
            items(watching) { e -> DiaryRow(e, onOpenFilm) }
        }
        item { Eyebrow("Watchlist", modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)) }
        items(watchlist) { e -> DiaryRow(e, onOpenFilm) }
    }
}
