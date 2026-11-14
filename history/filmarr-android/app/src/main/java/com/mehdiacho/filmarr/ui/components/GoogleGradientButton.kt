package com.mehdiacho.filmarr.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

/**
 * "Continue with Google" with a slowly drifting Google-colors gradient (Gemini-live vibe), ported
 * from yap. White logo chip + bold white label stay legible over the moving colors. The gradient
 * tile drifts steadily leftward by exactly one period, then loops; TileMode.Repeated + a seamless
 * first==last color make the wrap invisible.
 */
@Composable
fun GoogleGradientButton(
    text: String = "Continue with Google",
    busy: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "gg")
    val shift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(7200, easing = LinearEasing)), label = "ggShift",
    )
    val colors = listOf(GoogleBlue, GoogleRed, GoogleYellow, GoogleGreen, GoogleBlue)
    val span = 900f
    val off = -shift * span
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors,
                    start = Offset(off, 0f),
                    end = Offset(off + span, 0f),
                    tileMode = TileMode.Repeated,
                )
            )
            .clickable(enabled = !busy) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = GoogleBlue)
                else Text("G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (busy) "Signing you in…" else text,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
            )
        }
    }
}
