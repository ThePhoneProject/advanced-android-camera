package co.stonephone.stonecamera.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.utils.OrientationListener

@Composable
fun ResponsiveOrientation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val orientationAngle = remember { mutableStateOf(0) }

    val animatedRotation by animateFloatAsState(
        targetValue = orientationAngle.value.toFloat(),
        animationSpec = tween(durationMillis = 300) // 300ms animation duration
    )

    DisposableEffect(Unit) {
        val orientationListener = OrientationListener(MyApplication.getAppContext()) { angle ->
            orientationAngle.value = angle
        }

        orientationListener.enable()
        onDispose { orientationListener.disable() }
    }

    return Box(
        modifier = modifier.rotate(animatedRotation)
    ) {
        content()
    }
}