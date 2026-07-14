package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.ActionCircle
import com.mehdiacho.filmarr.ui.components.StarRow
import com.mehdiacho.filmarr.ui.components.SwipeDeck
import com.mehdiacho.filmarr.ui.components.rememberSwipeDeckState
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun DiscoverScreen(onOpenFilm: (String) -> Unit, onOpenAsk: () -> Unit) {
    val deck = remember { MockCatalog.discoverDeck }
    val deckState = rememberSwipeDeckState()
    val library by FilmRepository.library.collectAsStateWithLifecycle()
    val heartedIds = library.filter { it.favorite }.map { it.film.id }.toSet()

    var persistentFocus by remember { mutableStateOf(false) }
    var oneShotFocus by remember { mutableStateOf(false) }
    val focusActive = persistentFocus || oneShotFocus
    var rating by remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        ScreenHeader(
            title = "Discover",
            subtitle = "Swipe to curate films",
            actions = {
                ActionCircle(onClick = onOpenAsk) {
                    Icon(Icons.Filled.HelpOutline, "Ask filmarr", tint = FilmarrColors.TextTertiary, modifier = Modifier.size(18.dp))
                }
                ActionCircle(onClick = { persistentFocus = !persistentFocus }) {
                    Icon(
                        Icons.Filled.TrackChanges, "Focus mode",
                        tint = if (persistentFocus) FilmarrColors.Rose else FilmarrColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )

        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 78.dp)) {
            SwipeDeck(
                films = deck,
                state = deckState,
                rightLabel = "INTERESTED",
                leftLabel = "SKIP",
                heartedIds = heartedIds,
                onSwipeRight = { film ->
                    if (focusActive && rating > 0f) FilmRepository.logWatched(film, rating) else FilmRepository.markInterested(film)
                    oneShotFocus = false; rating = 0f
                },
                onSwipeLeft = { oneShotFocus = false; rating = 0f },
                onTap = { onOpenFilm(it.id) },
                onDoubleTap = { FilmRepository.toggleFavorite(it) },
                onLongPress = { if (!persistentFocus) oneShotFocus = !oneShotFocus },
                modifier = Modifier.fillMaxSize(),
            )

            // Focus review panel over the lower part of the card.
            if (focusActive) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    FocusReviewPanel(rating = rating, onRate = { rating = it })
                }
            }

            // Undo FAB
            Box(
                Modifier.align(Alignment.BottomEnd).padding(bottom = 14.dp)
                    .size(50.dp).clip(RoundedCornerShape(Dimens.rFull))
                    .background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rFull))
                    .clickable { deckState.undo(); FilmRepository.undo() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun FocusReviewPanel(rating: Float, onRate: (Float) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = Dimens.rXl, bottomEnd = Dimens.rXl))
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "SWIPE RIGHT TO LOG THIS REVIEW",
            style = FilmarrType.eyebrow, color = FilmarrColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
        StarRow(rating = rating, star = 26.dp, gap = 6.dp, onRate = onRate, modifier = Modifier.padding(vertical = 12.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.rMd))
                .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rMd))
                .padding(horizontal = 13.dp, vertical = 11.dp),
        ) {
            Text("Add a note… (optional)", style = FilmarrType.body, color = FilmarrColors.TextTertiary)
        }
    }
}
