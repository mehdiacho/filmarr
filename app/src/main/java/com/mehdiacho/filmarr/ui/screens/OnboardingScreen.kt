@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.GenreChip
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.components.ProgressTrack
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import androidx.compose.material3.Text

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val selected = remember { mutableStateListOf(*MockCatalog.onboardingGenres.filter { it.selected }.map { it.name }.toTypedArray()) }
    val genres = remember { MockCatalog.onboardingGenres.map { it.name } }

    Column(
        Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Text("Tune your\ntaste.", style = FilmarrType.display, color = FilmarrColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pick the genres you love — we'll seed your very first deck. No searching required, ever.",
            style = FilmarrType.body, color = FilmarrColors.TextTertiary,
        )
        Spacer(Modifier.height(22.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            genres.forEach { g ->
                GenreChip(g, selected = g in selected, onClick = {
                    if (g in selected) selected.remove(g) else selected.add(g)
                })
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressTrack(progress = (selected.size.coerceAtMost(5)) / 5f, modifier = Modifier.weight(1f), height = 4.dp)
            Text("${selected.size.coerceAtMost(5)} / 5", style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
        }
        PrimaryCta("Build my deck", onClick = onDone)
        Box(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Text(
                "Skip — surprise me", style = FilmarrType.meta, color = FilmarrColors.TextMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(4.dp),
            )
        }
    }
}
