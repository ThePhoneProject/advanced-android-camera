package co.stonephone.stonecamera.ui

import android.content.Context
import android.graphics.Rect
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
import androidx.compose.ui.zIndex

@Composable
fun FocusReticle(
    x: Float,
    y: Float,
    initialBrightness: Float = 0f,
    onDismissFocus: () -> Unit,
    onSetBrightness: (Float) -> Unit,
    visibleDimensions: Rect,
    context: Context
) {
    var brightness by remember { mutableStateOf(initialBrightness) }
    val focusReticleSize = 70
    val reticleHalf = focusReticleSize / 2
    val minY = visibleDimensions!!.top + 10f
    val maxY = visibleDimensions!!.bottom - (focusReticleSize * 2f + 10)
    val minX = visibleDimensions!!.left + 10f
    val maxX = visibleDimensions!!.right - (focusReticleSize * 2f + 10)

    Box(
        modifier = Modifier
//            .offset(x.dp - reticleHalf.dp, y.dp - reticleHalf.dp)
            .offset(
                pxToDp(maxOf(minOf(x, maxX), minX) - reticleHalf, context).dp,
                pxToDp(maxOf(minOf(y, maxY), minY) - reticleHalf, context).dp
            )
            .size(focusReticleSize.dp)
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
                .zIndex(2f)
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
                    .width(focusReticleSize.dp)
                    .height(5.dp)
                    .padding(0.dp)
            )
        }
    }
}

fun pxToDp(px: Float, context: Context): Float {
    val density = context.resources.displayMetrics.density
    return px / density
}