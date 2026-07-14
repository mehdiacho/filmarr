package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.ui.components.FilmarrToggle
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenLetterboxd: () -> Unit, onOpenAuth: () -> Unit) {
    var sync by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
            Text("Settings", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
        }
        SettingRow("Account", "Sign in to sync your library", onClick = onOpenAuth)
        SettingRow("Import from Letterboxd", "Bring your history over", onClick = onOpenLetterboxd)
        ToggleRow("Auto-sync to Letterboxd", sync) { sync = it }
        SettingRow("About filmarr", "Version 1.0", onClick = {})
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(Dimens.rMd))
            .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary)
            Text(subtitle, style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = FilmarrColors.TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(Dimens.rMd))
            .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = FilmarrType.labelStrong, color = FilmarrColors.TextPrimary, modifier = Modifier.weight(1f))
        FilmarrToggle(checked, onChange)
    }
}
