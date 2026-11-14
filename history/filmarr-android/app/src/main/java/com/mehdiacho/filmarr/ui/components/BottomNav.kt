package com.mehdiacho.filmarr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.navigation.Dest
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

/** Persistent root nav + center Quick-Log FAB (§2.1). */
@Composable
fun FilmarrBottomNav(
    current: String?,
    onNavigate: (String) -> Unit,
    onQuickLog: () -> Unit,
    onQuickLogLongPress: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(FilmarrColors.Bg)) {
        Box(
            Modifier.fillMaxWidth().border(0.dp, Color.Transparent)
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem(Icons.Filled.Explore, Icons.Outlined.Explore, "Discover", current == Dest.DISCOVER) { onNavigate(Dest.DISCOVER) }
                NavItem(Icons.Filled.Layers, Icons.Outlined.Layers, "Build", current == Dest.BUILD) { onNavigate(Dest.BUILD) }
                Box(Modifier.size(56.dp)) // FAB spacer
                NavItem(Icons.Filled.GridView, Icons.Outlined.GridView, "Library", current == Dest.LIBRARY) { onNavigate(Dest.LIBRARY) }
                NavItem(Icons.Filled.Person, Icons.Outlined.Person, "Profile", current == Dest.PROFILE) { onNavigate(Dest.PROFILE) }
            }
            // center FAB, raised -18dp
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = (-18).dp)
                    .size(46.dp).clip(RoundedCornerShape(Dimens.rFull))
                    .background(FilmarrColors.Rose)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onQuickLog() },
                            onLongPress = { onQuickLogLongPress() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, "Quick log", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        // draw behind the gesture pill
        Box(Modifier.background(FilmarrColors.Bg).windowInsetsBottomHeight(WindowInsets.navigationBars).fillMaxWidth())
    }
}

@Composable
private fun NavItem(selected: ImageVector, unselected: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (active) selected else unselected, label,
            tint = if (active) FilmarrColors.TextPrimary else FilmarrColors.TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Box(Modifier.height(3.dp))
        Text(label, style = FilmarrType.navLabel, color = if (active) FilmarrColors.TextPrimary else FilmarrColors.TextMuted)
    }
}
