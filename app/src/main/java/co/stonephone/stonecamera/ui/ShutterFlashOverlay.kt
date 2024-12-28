package co.stonephone.stonecamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ShutterFlashOverlay(onFlashComplete: () -> Unit) {
    // Animatable to track the alpha (0f..1f)
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    // Launch the animation once this composable enters the composition
    LaunchedEffect(alphaAnim) {
        // Start fully transparent
        alphaAnim.snapTo(0f)

        // Animate up to 0.7 in ~100ms (quick bright flash)
        alphaAnim.animateTo(
            targetValue = 0.7f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)
        )
        // Then animate back down to 0f in ~200ms
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
        )
        // Once done, notify the parent
        onFlashComplete()
    }

    // A full-screen Box that dims/brightens based on alpha
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White.copy(alpha = alphaAnim.value))
    )
}
