package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.auth.AuthManager
import com.mehdiacho.filmarr.data.recommender.RecommenderRepository
import com.mehdiacho.filmarr.ui.components.FilmarrToggle
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLetterboxd: () -> Unit,
    onOpenAuth: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val user by AuthManager.user.collectAsStateWithLifecycle()
    val signedIn = user != null
    var sync by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
            Text("Settings", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
        }
        SettingRow(
            "Account",
            if (signedIn) (user?.email ?: "Signed in") else "Sign in to sync your library",
            onClick = onOpenAuth,
        )
        SettingRow("Import from Letterboxd", "Bring your history over", onClick = onOpenLetterboxd)
        ToggleRow("Auto-sync to Letterboxd", sync) { sync = it }
        SettingRow("About filmarr", "Version 1.0", onClick = {})
        if (signedIn) {
            Spacer(Modifier.height(6.dp))
            DangerRow("Delete account", "Permanently erase your account and data", onClick = { confirmDelete = true })
        }
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            onDismiss = { confirmDelete = false },
            onDeleted = {
                confirmDelete = false
                onAccountDeleted()
            },
        )
    }
}

/** Two-step destructive confirmation. Calls the backend, then signs out locally on success. */
@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        containerColor = FilmarrColors.SheetBg,
        titleContentColor = FilmarrColors.TextPrimary,
        textContentColor = FilmarrColors.TextTertiary,
        title = { Text("Delete account?", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary) },
        text = {
            Column {
                Text(
                    "This permanently erases your account and everything tied to it — your library, " +
                        "hearts, ratings, watch history, and taste profile. This cannot be undone.",
                    style = FilmarrType.body, color = FilmarrColors.TextTertiary,
                )
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("Couldn't delete your account: $it", style = FilmarrType.meta, color = FilmarrColors.Rose)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !deleting,
                onClick = {
                    deleting = true
                    error = null
                    scope.launch {
                        RecommenderRepository.deleteAccount()
                            .onSuccess {
                                // Data + auth user are gone server-side; drop local state and the session.
                                FilmRepository.unbind()
                                AuthManager.signOut()
                                onDeleted()
                            }
                            .onFailure {
                                error = it.message ?: "Unknown error"
                                deleting = false
                            }
                    }
                },
            ) {
                Text(if (deleting) "Deleting…" else "Delete forever", color = FilmarrColors.Rose)
            }
        },
        dismissButton = {
            TextButton(enabled = !deleting, onClick = onDismiss) {
                Text("Cancel", color = FilmarrColors.TextSecondary)
            }
        },
    )
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
private fun DangerRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(Dimens.rMd))
            .background(FilmarrColors.SurfaceCard).border(1.dp, FilmarrColors.Rose.copy(alpha = 0.5f), RoundedCornerShape(Dimens.rMd))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FilmarrType.labelStrong, color = FilmarrColors.Rose)
            Text(subtitle, style = FilmarrType.meta, color = FilmarrColors.TextTertiary)
        }
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
