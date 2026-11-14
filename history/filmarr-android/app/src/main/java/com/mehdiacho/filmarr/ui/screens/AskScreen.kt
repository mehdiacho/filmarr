package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.ChatMessage
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.MockCatalog
import com.mehdiacho.filmarr.ui.components.Eyebrow
import com.mehdiacho.filmarr.ui.components.ResultRow
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun AskScreen(onBack: () -> Unit, onOpenFilm: (String) -> Unit) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(true, "That radio-static mystery everyone was posting about"),
            ChatMessage(false, "Sounds like Velvet Static (2024) — a slow-burn about a coastal radio engineer. Here it is:", filmResults = listOf(MockCatalog.byId("velvet-static"))),
        )
    }
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackCircle(onBack = onBack)
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239)))))
            Text("Ask filmarr", style = FilmarrType.screenTitle, color = FilmarrColors.TextPrimary)
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(messages)
            item {
                Eyebrow("Try asking", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MockCatalog.askSuggestions.forEach { s ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
                                .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd)).clickable { input = s }.padding(14.dp),
                        ) { Text(s, style = FilmarrType.body, color = FilmarrColors.TextSecondary) }
                    }
                }
            }
        }

        // Input bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).imePadding().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(Dimens.rLg)).background(FilmarrColors.SurfaceCard)
                    .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rLg)).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isEmpty()) Text("Describe a film…", style = FilmarrType.body, color = FilmarrColors.TextTertiary)
                BasicTextField(
                    value = input, onValueChange = { input = it }, singleLine = true,
                    cursorBrush = SolidColor(FilmarrColors.Rose),
                    textStyle = FilmarrType.body.copy(color = FilmarrColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(Dimens.rLg)).background(FilmarrColors.Rose).clickable {
                    if (input.isNotBlank()) {
                        messages.add(ChatMessage(true, input))
                        messages.add(ChatMessage(false, "Here's a match for that:", filmResults = listOf(MockCatalog.films.random())))
                        input = ""
                    }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

// helper to render chat items with film results in the LazyColumn
private fun androidx.compose.foundation.lazy.LazyListScope.items(messages: List<ChatMessage>) {
    items(messages.size) { i ->
        val m = messages[i]
        ChatBubble(m)
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    Column(Modifier.fillMaxWidth()) {
        if (m.fromUser) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)).background(FilmarrColors.Blue).padding(horizontal = 14.dp, vertical = 10.dp),
                ) { Text(m.text, style = FilmarrType.body, color = Color.White) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(Dimens.rFull)).background(Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239)))))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(m.text, style = FilmarrType.body, color = FilmarrColors.TextSecondary)
                    m.filmResults.forEach { film ->
                        ResultRow(film = film, logged = FilmRepository.isLogged(film.id), onClick = {}, onAdd = { FilmRepository.logWatched(film) })
                    }
                }
            }
        }
    }
}
