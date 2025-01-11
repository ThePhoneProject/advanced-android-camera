package co.stonephone.stonecamera.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.StoneCameraViewModel

class PhotoModePlugin : IPlugin {
    override val id: String = "photoMode"
    override val name: String = "Photo Mode"

    private lateinit var viewModel: StoneCameraViewModel

    override val modeLabel
        get() = "photo"

    override val renderModeControl
        get() = @Composable {
            // Bottom row (Flip + Shutter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Transparent, shape = CircleShape)
                        .clickable {
                            viewModel.toggleCameraFacing()
                        }, contentAlignment = Alignment.Center
                ) {

                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(1.dp, Color.White, CircleShape)
                        .padding(4.dp),

                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White,
                                shape = CircleShape
                            )
                            .clickable {
                                // Then capture the photo
                                viewModel.capturePhoto(
                                    viewModel.imageCapture,
                                )

                                // Trigger the shutter flash overlay
                                viewModel.triggerShutterFlash()
                            }
                    ) {}
                }

                // Camera Switcher Button
                IconButton(
                    onClick = { viewModel.toggleCameraFacing() },
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlipCameraAndroid, // Use FlipCameraAndroid if preferred
                        contentDescription = "Flip Camera",
                        tint = Color.White, // Customize the color if needed
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
    }
}
