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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehdiacho.filmarr.data.auth.AuthManager
import com.mehdiacho.filmarr.data.auth.signInWithGoogle
import com.mehdiacho.filmarr.ui.components.GoogleGradientButton
import com.mehdiacho.filmarr.ui.components.PrimaryCta
import com.mehdiacho.filmarr.ui.theme.Dimens
import com.mehdiacho.filmarr.ui.theme.FilmarrColors
import com.mehdiacho.filmarr.ui.theme.FilmarrType
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onBack: () -> Unit, onAuthed: () -> Unit, onGuest: (() -> Unit)? = null) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun handle(result: Result<Unit>) {
        busy = false
        result.onSuccess { onAuthed() }.onFailure { error = it.message ?: "Something went wrong" }
    }

    fun submitEmail() {
        if (email.isBlank() || password.length < 6) { error = "Enter an email and a 6+ character password"; return }
        error = null; busy = true
        scope.launch { handle(if (isSignUp) AuthManager.signUpEmail(email, password) else AuthManager.signInEmail(email, password)) }
    }

    fun submitGoogle() {
        error = null; busy = true
        scope.launch { handle(signInWithGoogle(context)) }
    }

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

        AuthField("Email", email, { email = it })
        Spacer(Modifier.height(10.dp))
        AuthField("Password", password, { password = it }, isPassword = true)

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, style = FilmarrType.meta, color = FilmarrColors.Rose, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryCta(if (busy) "Please wait…" else if (isSignUp) "Sign up" else "Sign in", onClick = { if (!busy) submitEmail() })
        Text(
            if (isSignUp) "Already have an account? Sign in" else "New here? Create an account",
            style = FilmarrType.meta, color = FilmarrColors.Rose,
            modifier = Modifier.clickable { isSignUp = !isSignUp }.padding(12.dp),
        )

        // Docked bottom: "or" divider → animated-gradient Google button → guest link.
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = FilmarrColors.Hairline)
            Text("  or  ", style = FilmarrType.meta, color = FilmarrColors.TextMuted)
            HorizontalDivider(Modifier.weight(1f), color = FilmarrColors.Hairline)
        }
        Spacer(Modifier.height(14.dp))
        GoogleGradientButton(busy = busy) { if (!busy) submitGoogle() }
        if (onGuest != null) {
            Text(
                "Browse films without an account",
                style = FilmarrType.meta, color = FilmarrColors.TextTertiary,
                modifier = Modifier.clickable { onGuest() }.padding(top = 10.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
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
