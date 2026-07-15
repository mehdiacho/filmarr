package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.data.auth.AuthManager
import com.mehdiacho.filmarr.ui.components.ActionCircle
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenGraph: () -> Unit,
    onOpenOverlap: () -> Unit,
    onOpenLetterboxd: () -> Unit,
    onOpenAuth: () -> Unit,
) {
    val stats = MockCatalog.profileStats
    val user by AuthManager.user.collectAsStateWithLifecycle()
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email?.substringBefore("@") ?: "Guest"
    val handle = user?.email ?: "Not signed in · tap to sync"
    Column(
        Modifier.fillMaxSize().background(FilmarrColors.Bg).verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        // Identity row
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239)))))
            Column(Modifier.weight(1f)) {
                Text(displayName, style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
                Text(handle, style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
            }
            ActionCircle(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, "Settings", tint = FilmarrColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        // Stat cards
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${stats.filmsLogged}", "Films", Modifier.weight(1f))
            StatCard("${stats.reviews}", "Reviews", Modifier.weight(1f))
            StatCard(String.format("%.1f", stats.avgRating), "Avg", Modifier.weight(1f))
        }

        // Quick links
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LinkChip("Taste graph", Modifier.weight(1f), onOpenGraph)
            LinkChip("Overlap", Modifier.weight(1f), onOpenOverlap)
            LinkChip("Import", Modifier.weight(1f), onOpenLetterboxd)
        }

        // Ratings histogram
        Eyebrow("Ratings spread", modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
        Row(
            Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val max = stats.ratingHistogram.maxOrNull() ?: 1
            stats.ratingHistogram.forEachIndexed { i, v ->
                val peak = v == max
                Box(
                    Modifier.weight(1f).fillMaxHeight((v.toFloat() / max).coerceIn(0.04f, 1f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (peak) FilmarrColors.Gold else FilmarrColors.Rose.copy(alpha = 0.6f))
                )
            }
        }

        // Top genres
        Eyebrow("Top genres", modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.topGenres.forEach { (name, share) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(name, style = FilmarrType.meta, color = FilmarrColors.TextSecondary, modifier = Modifier.size(width = 70.dp, height = 18.dp))
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(Dimens.rFull)).background(FilmarrColors.SurfaceCard)) {
                        Box(Modifier.fillMaxWidth(share).height(8.dp).clip(RoundedCornerShape(Dimens.rFull)).background(FilmarrColors.Rose))
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            if (user != null) {
                Text("Sign out", style = FilmarrType.meta, color = FilmarrColors.Rose, modifier = Modifier.clickable { AuthManager.signOut() })
            } else {
                Text("Sign in to sync →", style = FilmarrType.meta, color = FilmarrColors.TextMuted, modifier = Modifier.clickable(onClick = onOpenAuth))
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(Dimens.rLg)).background(FilmarrColors.SurfaceRaised)
            .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rLg)).padding(vertical = 16.dp, horizontal = 14.dp),
    ) {
        Text(value, style = FilmarrType.statNumeral.copy(fontSize = 26.sp), color = FilmarrColors.TextPrimary)
        Text(label, style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
    }
}

@Composable
private fun LinkChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
            .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd)).clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = FilmarrType.meta, color = FilmarrColors.TextSecondary) }
}
