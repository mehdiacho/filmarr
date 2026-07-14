package com.mehdiacho.filmarr.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDir { LEFT, RIGHT }

/** Hoisted deck state so on-screen buttons + undo FAB can drive the deck (§2.4). */
class SwipeDeckState {
    var index by mutableIntStateOf(0)
        internal set
    internal var pending by mutableStateOf<SwipeDir?>(null)
    internal val history = mutableStateListOf<SwipeDir>()

    fun swipeLeft() { pending = SwipeDir.LEFT }
    fun swipeRight() { pending = SwipeDir.RIGHT }
    fun undo() {
        if (history.isNotEmpty()) { history.removeAt(history.lastIndex); if (index > 0) index-- }
    }
    val canUndo: Boolean get() = history.isNotEmpty()
}

@Composable
fun rememberSwipeDeckState() = remember { SwipeDeckState() }

private val SpringEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * The swipe deck (§2.4). Three visible layers; the top card is draggable with the exact
 * transform + commit rules from the spec. R/L labels differ per feed (Discover vs Build).
 */
@Composable
fun SwipeDeck(
    films: List<Film>,
    state: SwipeDeckState,
    onSwipeRight: (Film) -> Unit,
    onSwipeLeft: (Film) -> Unit,
    modifier: Modifier = Modifier,
    rightLabel: String = "INTERESTED",
    leftLabel: String = "SKIP",
    heartedIds: Set<String> = emptySet(),
    onTap: (Film) -> Unit = {},
    onDoubleTap: (Film) -> Unit = {},
    onLongPress: (Film) -> Unit = {},
    pill: (Film) -> String? = { null },
    emptyState: @Composable () -> Unit = { DefaultEmpty() },
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thresholdPx = with(density) { 80.dp.toPx() }
        val progressPx = with(density) { 90.dp.toPx() }
        val scope = rememberCoroutineScope()
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }

        suspend fun commit(dir: SwipeDir) {
            val film = films.getOrNull(state.index) ?: return
            val target = if (dir == SwipeDir.RIGHT) widthPx * 1.6f else -widthPx * 1.6f
            offsetX.animateTo(target, tween(300))
            if (dir == SwipeDir.RIGHT) onSwipeRight(film) else onSwipeLeft(film)
            state.history.add(dir)
            state.index += 1
            offsetX.snapTo(0f); offsetY.snapTo(0f)
        }

        // Programmatic swipes (on-screen buttons / keyboard).
        LaunchedEffect(state.pending) {
            val dir = state.pending ?: return@LaunchedEffect
            if (state.index < films.size) commit(dir)
            state.pending = null
        }

        if (state.index >= films.size) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { emptyState() }
            return@BoxWithConstraints
        }

        val top = films[state.index]
        val next = films.getOrNull(state.index + 1)
        val third = films.getOrNull(state.index + 2)

        // third layer
        if (third != null) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = 0.90f; scaleY = 0.90f; translationY = 26.dp.toPx(); alpha = 0.5f }
                    .clip(RoundedCornerShape(Dimens.rXl))
            ) { PosterSurface(third, Modifier.fillMaxSize(), ghostTitle = false) }
        }
        // next layer
        if (next != null) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = 0.95f; scaleY = 0.95f; translationY = 13.dp.toPx() }
                    .clip(RoundedCornerShape(Dimens.rXl))
            ) { SwipeCardFace(next, hearted = next.id in heartedIds, pill = pill(next)) }
        }
        // top draggable layer
        val rightAlpha = (offsetX.value / progressPx).coerceIn(0f, 1f)
        val leftAlpha = (-offsetX.value / progressPx).coerceIn(0f, 1f)
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value * 0.25f
                    rotationZ = offsetX.value / 300f * 16f
                }
                .clip(RoundedCornerShape(Dimens.rXl))
                .pointerInput(state.index) {
                    detectTapGestures(
                        onTap = { onTap(top) },
                        onDoubleTap = { onDoubleTap(top) },
                        onLongPress = { onLongPress(top) },
                    )
                }
                .pointerInput(state.index) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                            scope.launch { offsetY.snapTo(offsetY.value + drag.y) }
                        },
                        onDragEnd = {
                            when {
                                offsetX.value > thresholdPx -> scope.launch { commit(SwipeDir.RIGHT) }
                                offsetX.value < -thresholdPx -> scope.launch { commit(SwipeDir.LEFT) }
                                else -> {
                                    scope.launch { offsetX.animateTo(0f, tween(420, easing = SpringEasing)) }
                                    scope.launch { offsetY.animateTo(0f, tween(420, easing = SpringEasing)) }
                                }
                            }
                        },
                    )
                }
        ) {
            SwipeCardFace(top, hearted = top.id in heartedIds, pill = pill(top))
            DirectionalBadge(
                rightLabel, FilmarrColors.Green, rotation = -12f, alpha = rightAlpha,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 78.dp),
            )
            DirectionalBadge(
                leftLabel, FilmarrColors.Rose, rotation = 12f, alpha = leftAlpha,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 78.dp),
            )
        }
    }
}

/** The card face — art + heart + bottom meta block (§2.4). Optional [pill] = "Likely seen" (Build). */
@Composable
fun SwipeCardFace(film: Film, hearted: Boolean, modifier: Modifier = Modifier, pill: String? = null) {
    Box(modifier.fillMaxSize()) {
        PosterSurface(film, Modifier.fillMaxSize())
        // heart
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 16.dp)
                .size(30.dp).clip(RoundedCornerShape(Dimens.rFull))
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite, null,
                tint = if (hearted) FilmarrColors.Rose else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(15.dp),
            )
        }
        // meta block
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 20.dp)
        ) {
            if (pill != null) {
                androidx.compose.foundation.layout.Row(
                    Modifier.clip(RoundedCornerShape(Dimens.rFull))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, FilmarrColors.HairlineStrong, RoundedCornerShape(Dimens.rFull))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(Dimens.rFull)).background(FilmarrColors.Green))
                    Text(pill, style = FilmarrType.meta.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)), color = Color.White)
                }
                Box(Modifier.height(10.dp))
            }
            Text(film.title, style = FilmarrType.cardTitle, color = Color.White)
            Box(Modifier.height(6.dp))
            MetaRow(film)
        }
    }
}

@Composable
private fun DefaultEmpty() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("You're all caught up", style = FilmarrType.cardTitle, color = FilmarrColors.TextSecondary)
        Box(Modifier.height(6.dp))
        Text("Come back later for more picks", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
    }
}
