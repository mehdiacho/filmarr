package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.data.WatchStatus
import com.mehdiacho.filmarr.data.tmdb.TmdbRepository
import com.mehdiacho.filmarr.data.tmdb.backdropUrl
import com.mehdiacho.filmarr.data.tmdb.posterUrl
import com.mehdiacho.filmarr.ui.components.ConfirmButton
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.IconSquareButton
import com.mehdiacho.filmarr.ui.components.StarRow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import com.mehdiacho.filmarr.ui.theme.PosterGradients

@Composable
fun FilmDetailScreen(
    filmId: String,
    onBack: () -> Unit,
    onOpenFilm: (String) -> Unit,
    canLog: Boolean = true,
    onRequireAuth: () -> Unit = {},
) {
    val fallback = remember(filmId) {
        TmdbRepository.cached(filmId) ?: runCatching { MockCatalog.byId(filmId) }.getOrDefault(MockCatalog.films.first())
    }
    var film by remember(filmId) { mutableStateOf(fallback) }
    var moreLikeThis by remember(filmId) { mutableStateOf<List<Film>>(emptyList()) }
    LaunchedEffect(filmId) {
        TmdbRepository.detail(filmId)?.let { film = it }
        moreLikeThis = TmdbRepository.recommendations(filmId)
    }
    val library by FilmRepository.library.collectAsStateWithLifecycle()
    val entry = library.firstOrNull { it.film.id == film.id }
    val watched = entry?.status == WatchStatus.WATCHED

    Box(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Backdrop
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Box(Modifier.fillMaxSize().background(PosterGradients.forKey(film.gradientKey).brush))
                (film.backdropUrl() ?: film.posterUrl("w780"))?.let { art ->
                    AsyncImage(
                        model = art, contentDescription = film.title,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to FilmarrColors.Bg)
                    )
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 100.dp)) {
                Text(film.title, style = FilmarrType.display.copy(fontSize = 32.sp), color = FilmarrColors.TextPrimary)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${film.year}", style = FilmarrType.meta, color = FilmarrColors.TextSecondary)
                    Text("·", style = FilmarrType.meta, color = FilmarrColors.TextMuted)
                    Text(film.genres.first(), style = FilmarrType.meta, color = FilmarrColors.TextSecondary)
                    Text("·", style = FilmarrType.meta, color = FilmarrColors.TextMuted)
                    Text("${film.runtimeMin / 60}h ${film.runtimeMin % 60}m", style = FilmarrType.meta, color = FilmarrColors.TextSecondary)
                }
                // Rating row
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(String.format("%.1f", film.rating), style = FilmarrType.statNumeral.copy(fontSize = 30.sp), color = FilmarrColors.Gold)
                        Text("TMDB", style = FilmarrType.meta, color = FilmarrColors.TextMuted, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Box(Modifier.width(1.dp).height(32.dp).background(FilmarrColors.HairlineStrong))
                    Column {
                        Eyebrow("Your rating")
                        StarRow(rating = entry?.userRating ?: 0f, star = 16.dp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                // Action row
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfirmButton(
                        text = if (watched) "Watched" else "Watched",
                        onClick = { if (canLog) FilmRepository.logWatched(film) else onRequireAuth() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, null, tint = FilmarrColors.OnGreen, modifier = Modifier.size(15.dp).padding(end = 0.dp))
                        Box(Modifier.width(6.dp))
                    }
                    IconSquareButton(onClick = { if (canLog) FilmRepository.toggleFavorite(film) else onRequireAuth() }) {
                        Icon(Icons.Filled.Favorite, "Favorite", tint = if (entry?.favorite == true) FilmarrColors.Rose else FilmarrColors.Rose, modifier = Modifier.size(18.dp))
                    }
                    IconSquareButton(onClick = { if (canLog) FilmRepository.addToWatchlist(film) else onRequireAuth() }) {
                        Icon(Icons.Filled.Bookmark, "Watchlist", tint = FilmarrColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(film.overview, style = FilmarrType.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp), color = FilmarrColors.TextTertiary, modifier = Modifier.padding(top = 18.dp))

                Eyebrow("Cast", modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    film.cast.take(3).forEach { name ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(46.dp).clip(RoundedCornerShape(Dimens.rFull)).background(PosterGradients.forKey(name).brush))
                            Text(name, style = FilmarrType.meta.copy(fontSize = 9.sp), color = FilmarrColors.TextTertiary, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(Dimens.rFull)).background(FilmarrColors.SurfaceCard), contentAlignment = Alignment.Center) {
                        Text("+9", style = FilmarrType.labelStrong, color = FilmarrColors.TextTertiary)
                    }
                }

                Eyebrow("More like this", modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                val similar = (moreLikeThis.ifEmpty { MockCatalog.films }).filter { it.id != film.id }.take(4)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    similar.forEach { m ->
                        Box(
                            Modifier.size(width = 58.dp, height = 84.dp).clip(RoundedCornerShape(9.dp))
                                .background(PosterGradients.forKey(m.gradientKey).brush)
                                .clickable { onOpenFilm(m.id) }
                        ) {
                            m.posterUrl("w185")?.let { url ->
                                AsyncImage(
                                    model = url, contentDescription = m.title,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
        BackCircle(onBack = onBack, translucent = true, modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp))
    }
}
