@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mehdiacho.filmarr.data.AgeBracket
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.data.tmdb.TmdbRepository
import com.mehdiacho.filmarr.data.tmdb.posterUrl
import com.mehdiacho.filmarr.ui.components.GenreChip
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

// Coarse age brackets (optional). Values must match base.py / data.AgeBracket.
private val AGE_OPTIONS = listOf(
    "13–17" to AgeBracket.TEEN,
    "18–24" to AgeBracket.YOUNG_ADULT,
    "25–34" to AgeBracket.ADULT,
    "35–44" to AgeBracket.MID,
    "45–54" to AgeBracket.MATURE,
    "55+" to AgeBracket.SENIOR,
)

/**
 * Cold-start onboarding (GRAPH_RECOMMENDER.md §10). Collects the seed set a brand-new account
 * needs for a non-empty first feed: genre interests, **a few films they've loved** (written as
 * INTERESTED seed interactions), and an optional age bracket. "Skip" leaves the graph to warm up
 * from swipes instead.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val genres = remember { MockCatalog.onboardingGenres.map { it.name } }
    val selectedGenres = remember {
        mutableStateListOf(*MockCatalog.onboardingGenres.filter { it.selected }.map { it.name }.toTypedArray())
    }
    var films by remember { mutableStateOf<List<Film>>(emptyList()) }
    val lovedIds = remember { mutableStateListOf<String>() }
    var ageBracket by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        films = TmdbRepository.discover().ifEmpty { MockCatalog.discoverDeck }
    }

    fun finish(seed: Boolean) {
        if (seed) {
            val loved = films.filter { it.id in lovedIds }
            FilmRepository.completeOnboarding(selectedGenres.toList(), loved, ageBracket)
        }
        onDone()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(Modifier.height(24.dp))
                Text("Tune your\ntaste.", style = FilmarrType.display, color = FilmarrColors.TextPrimary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pick the genres you love — we'll seed your very first deck. No searching required, ever.",
                    style = FilmarrType.body, color = FilmarrColors.TextTertiary,
                )
                Spacer(Modifier.height(18.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    genres.forEach { g ->
                        GenreChip(g, selected = g in selectedGenres, onClick = {
                            if (g in selectedGenres) selectedGenres.remove(g) else selectedGenres.add(g)
                        })
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text("A few you've loved", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap films you've genuinely enjoyed — this is what your first recommendations grow from.",
                    style = FilmarrType.meta, color = FilmarrColors.TextTertiary,
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        items(films, key = { it.id }) { film ->
            PosterPick(film, selected = film.id in lovedIds, onToggle = {
                if (film.id in lovedIds) lovedIds.remove(film.id) else lovedIds.add(film.id)
            })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(Modifier.height(24.dp))
                Text("About you", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Optional — helps us see how your taste has changed over time so we recommend better.",
                    style = FilmarrType.meta, color = FilmarrColors.TextTertiary,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    AGE_OPTIONS.forEach { (label, value) ->
                        GenreChip(label, selected = ageBracket == value, onClick = {
                            ageBracket = if (ageBracket == value) null else value
                        })
                    }
                }
                Spacer(Modifier.height(26.dp))
                PrimaryCta(if (lovedIds.isEmpty()) "Build my deck" else "Build my deck (${lovedIds.size})", onClick = { finish(seed = true) })
                Text(
                    "Skip — surprise me", style = FilmarrType.meta, color = FilmarrColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { finish(seed = false) }.padding(14.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PosterPick(film: Film, selected: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(Dimens.rMd))
            .background(FilmarrColors.SurfaceCard)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) FilmarrColors.Rose else FilmarrColors.Hairline,
                shape = RoundedCornerShape(Dimens.rMd),
            )
            .clickable { onToggle() },
    ) {
        AsyncImage(
            model = film.posterUrl(),
            contentDescription = film.title,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.rMd)),
        )
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(Dimens.rFull))
                    .background(FilmarrColors.Rose).padding(3.dp),
            ) {
                Icon(Icons.Filled.Check, "Selected", tint = Color.White, modifier = Modifier.height(14.dp))
            }
        }
    }
}
