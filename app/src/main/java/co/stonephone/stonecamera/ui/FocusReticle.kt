package co.stonephone.stonecamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusReticle(
    x: Float,
    y: Float,
    initialBrightness: Float = 0f,
    onDismissFocus: () -> Unit,
    onSetBrightness: (Float) -> Unit
) {
    var brightness by remember { mutableStateOf(initialBrightness) }
    val focusReticleSize = 70.dp
    val reticleHalf = focusReticleSize / 2
    Box(
        modifier = Modifier
            .offset(x.dp - reticleHalf, y.dp - reticleHalf)
            .size(focusReticleSize)
            .background(Color.Transparent)
            .border(1.dp, Color(0xAAFFCC00))
    ) {
        // Dismiss Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFCC00))
                .size(20.dp)
                .clickable { onDismissFocus() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = Color.Black,
                fontSize = 14.sp
            )
        }

        // Brightness Slider
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 15.dp)
        ) {
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onSetBrightness(it)
                },
                valueRange = -1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFCC00),
                    activeTrackColor = Color(0xFFFFCC00),
                    inactiveTrackColor = Color.Gray
                ),
                modifier = Modifier
                    .width(focusReticleSize)
                    .height(5.dp)
                    .padding(0.dp)
            )
        }
    }
}
