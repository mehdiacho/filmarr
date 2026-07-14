package com.mehdiacho.filmarr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType

@Composable
fun AuthScreen(onBack: () -> Unit, onAuthed: () -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(FilmarrColors.Bg).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.align(Alignment.Start)) { BackCircle(onBack = onBack) }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(FilmarrColors.Rose, Color(0xFF9F1239)))))
        Spacer(Modifier.height(18.dp))
        Text(if (isSignUp) "Create your account" else "Welcome back", style = FilmarrType.display.copy(fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)), color = FilmarrColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Sync your library across devices.", style = FilmarrType.body, color = FilmarrColors.TextTertiary)
        Spacer(Modifier.height(26.dp))

        // Google button
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(Dimens.rLg)).background(Color.White).clickable { onAuthed() },
            contentAlignment = Alignment.Center,
        ) { Text("Continue with Google", style = FilmarrType.labelStrong, color = Color(0xFF1A1A1A)) }

        Spacer(Modifier.height(18.dp))
        Text("or", style = FilmarrType.meta, color = FilmarrColors.TextMuted)
        Spacer(Modifier.height(18.dp))

        AuthField("Email", email, { email = it })
        Spacer(Modifier.height(10.dp))
        AuthField("Password", password, { password = it }, isPassword = true)
        Spacer(Modifier.height(20.dp))
        PrimaryCta(if (isSignUp) "Sign up" else "Sign in", onClick = onAuthed)

        Spacer(Modifier.weight(1f))
        Text(
            if (isSignUp) "Already have an account? Sign in" else "New here? Create an account",
            style = FilmarrType.meta, color = FilmarrColors.Rose,
            modifier = Modifier.clickable { isSignUp = !isSignUp }.padding(12.dp),
        )
    }
}

@Composable
private fun AuthField(placeholder: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Dimens.rMd)).background(FilmarrColors.SurfaceCard)
            .border(1.dp, FilmarrColors.Hairline, RoundedCornerShape(Dimens.rMd)).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, style = FilmarrType.body, color = FilmarrColors.TextTertiary)
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            cursorBrush = SolidColor(FilmarrColors.Rose),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = FilmarrType.body.copy(color = FilmarrColors.TextPrimary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
