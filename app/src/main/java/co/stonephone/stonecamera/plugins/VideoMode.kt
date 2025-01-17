package co.stonephone.stonecamera.plugins

import android.util.Log
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.ResponsiveOrientation

class VideoModePlugin : IPlugin {
    override val id: String = "videoMode"
    override val name: String = "Video Mode"

    private lateinit var viewModel: StoneCameraViewModel

    override fun onModeSelected(
        viewModel: StoneCameraViewModel,
        previousMode: String,
        nextMode: String
    ) {
        if (previousMode == modeLabel) {
            viewModel.stopRecording()
        }
    }

    override val modeUseCases: List<PluginUseCase>
        get() = listOf(PluginUseCase.VIDEO, PluginUseCase.PHOTO, PluginUseCase.ANALYSIS)

    override val modeLabel
        get() = "video"

    override val renderModeControl
        get() = @Composable {

            val isRecording by viewModel::isRecording
            val isPaused by viewModel::isPaused

            // Bottom row (Flip + Shutter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Switcher Button
                if (isRecording) {
                    ResponsiveOrientation {
                        IconButton(
                            onClick = {
                                if (isPaused) {
                                    viewModel.resumeRecording()
                                } else {
                                    viewModel.pauseRecording()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                                .border(1.dp, Color.White, CircleShape)
                                .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                        ) {
                            if (isPaused) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Resume Recording",
                                    tint = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = "Pause Recording",
                                    tint = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    ResponsiveOrientation {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Transparent, shape = CircleShape)
                                .clickable {
                                    viewModel.toggleCameraFacing()
                                }, contentAlignment = Alignment.Center
                        ) {

                        }
                    }
                }
                ResponsiveOrientation {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .padding(4.dp), contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier
                            .background(
                                Color.Red,
                                shape = if (!isRecording) CircleShape else RectangleShape
                            )
                            .fillMaxSize(if (!isRecording) 1f else 0.5f)
                            .clickable {
                                if (!isRecording) {
                                    // Start recording
                                    viewModel.startRecording(
                                        viewModel.videoCapture
                                    ) { uri ->
                                        Log.d(
                                            "StoneCameraApp", "Video saved to: $uri"
                                        )
                                    }
                                } else {
                                    // Stop recording
                                    viewModel.stopRecording()
                                }

                            }


                        ) {}
                    }
                }

                if (isRecording) {
                    // TODO only show this button if usecase is bound
                    // Snapshot Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .padding(4.dp),

                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.White, shape = CircleShape
                                )
                                .clickable {
                                    // Then capture the photo
                                    viewModel.capturePhoto()
                                }) {}
                    }
                } else {
                    ResponsiveOrientation {
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
            }
        }

    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
    }
}
