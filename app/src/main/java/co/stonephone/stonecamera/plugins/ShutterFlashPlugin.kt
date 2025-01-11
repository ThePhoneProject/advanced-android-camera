package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import co.stonephone.stonecamera.StoneCameraViewModel
import kotlinx.coroutines.delay

class ShutterFlashPlugin : IPlugin {
    override val id: String = "shutterFlashPlugin"
    override val name: String = "Shutter Flash"

    private var showShutterFlash by mutableStateOf(false)

    @Composable
    override fun renderViewfinder(
        viewModel: StoneCameraViewModel,
        pluginInstance: IPlugin
    ) {
        var isFadingOut by remember { mutableStateOf(false) }

        if (showShutterFlash) {
            LaunchedEffect(Unit) {
                // Wait for the fade-in duration
                delay(20)
                // Trigger fade-out
                isFadingOut = true
                // Wait for the fade-out duration
                delay(50)
                // Reset state
                showShutterFlash = false
                isFadingOut = false
            }
        }

        AnimatedVisibility(
            visible = showShutterFlash || isFadingOut,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(20)), // Fade in over 100ms
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(50)) // Fade out over 200ms
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.7f))
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
    }

    override fun onCaptureStarted(stoneCameraViewModel: StoneCameraViewModel) {
        showShutterFlash = true
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
