// StoneCameraApp.kt
package co.stonephone.stonecamera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import co.stonephone.stonecamera.plugins.AspectRatioPlugin
import co.stonephone.stonecamera.plugins.FlashPlugin
import co.stonephone.stonecamera.plugins.FocusBasePlugin
import co.stonephone.stonecamera.plugins.PinchToZoomPlugin
import co.stonephone.stonecamera.plugins.PluginLocation
import co.stonephone.stonecamera.plugins.SettingLocation
import co.stonephone.stonecamera.plugins.TapToFocusPlugin
import co.stonephone.stonecamera.plugins.ZoomBarPlugin
import co.stonephone.stonecamera.plugins.ZoomBasePlugin
import co.stonephone.stonecamera.ui.RenderPluginSetting
import co.stonephone.stonecamera.ui.ShutterFlashOverlay
import co.stonephone.stonecamera.ui.StoneCameraPreview
import co.stonephone.stonecamera.ui.ZoomBar
import co.stonephone.stonecamera.utils.getAllCamerasInfo

val shootModes = arrayOf("Photo", "Video")

val PLUGINS = listOf(
    ZoomBasePlugin(),
    ZoomBarPlugin(),
    FocusBasePlugin(),
    TapToFocusPlugin(),
    PinchToZoomPlugin(),
    FlashPlugin(),
    AspectRatioPlugin(),
)

@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun StoneCameraApp(
    cameraProvider: ProcessCameraProvider,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val stoneCameraViewModel = viewModel<StoneCameraViewModel>(
        factory = StoneCameraViewModelFactory(context, lifecycleOwner, PLUGINS)
    )

    val isRecording = stoneCameraViewModel.isRecording
    val selectedMode = stoneCameraViewModel.selectedMode
    val showShutterFlash = stoneCameraViewModel.showShutterFlash

    val plugins by remember { stoneCameraViewModel::plugins }

    val viewfinderPlugins =
        plugins.filter { it.pluginLocation == PluginLocation.VIEWFINDER }
    val trayPlugins =
        plugins.filter { it.pluginLocation == PluginLocation.TRAY }

    LaunchedEffect(cameraProvider, lifecycleOwner) {
        stoneCameraViewModel.onCameraProvider(cameraProvider)
        stoneCameraViewModel.onLifecycleOwner(lifecycleOwner)
    }

    // We can load cameras once (or whenever context changes) and pass them to the ViewModel
    LaunchedEffect(Unit) {
        val allCameras = getAllCamerasInfo(context)
        stoneCameraViewModel.loadCameras(allCameras)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview behind everything
        StoneCameraPreview(
            onPreviewView = { pView -> stoneCameraViewModel.onPreviewView(pView) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {


            stoneCameraViewModel.pluginSettings.filter { it.renderLocation == SettingLocation.TOP }
                .map { setting ->
                    RenderPluginSetting(
                        setting,
                        stoneCameraViewModel,
                        modifier = Modifier.padding(4.dp)
                    )
                }
        }


        viewfinderPlugins.map {
            it.render(stoneCameraViewModel, it)
        }

        // If showShutterFlash is true, display the overlay
        if (showShutterFlash) {
            ShutterFlashOverlay(onFlashComplete = {
                stoneCameraViewModel.onShutterFlashComplete()
            })
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            trayPlugins.map {
                it.render(stoneCameraViewModel, it)
            }

            // Translucent overlay for mode switch & shutter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Mode selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        shootModes.forEach { mode ->
                            Text(
                                text = mode.uppercase(),
                                color = if (mode == selectedMode) Color(0xFFFFCC00) else Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    stoneCameraViewModel.selectMode(mode)
                                }
                            )
                        }
                    }

                    // Bottom row (Flip + Shutter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera Switcher Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Transparent, shape = CircleShape)
                                .clickable {
                                    stoneCameraViewModel.toggleCameraFacing()
                                },
                            contentAlignment = Alignment.Center
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
                                modifier = if (isRecording) {
                                    Modifier
                                        .background(Color.Red)
                                        .fillMaxSize(0.5f)
                                } else {
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (selectedMode == "Video") Color(0xFFFF3B30) else Color.White,
                                            shape = CircleShape
                                        )


                                }.clickable {
                                    when (selectedMode) {
                                        "Photo" -> {
                                            // Then capture the photo
                                            stoneCameraViewModel.capturePhoto(
                                                stoneCameraViewModel.imageCapture,
                                            )

                                            // Trigger the shutter flash overlay
                                            stoneCameraViewModel.triggerShutterFlash()
                                        }

                                        "Video" -> {
                                            if (!isRecording) {
                                                // Start recording
                                                stoneCameraViewModel.startRecording(
                                                    stoneCameraViewModel.videoCapture
                                                ) { uri ->
                                                    Log.d(
                                                        "StoneCameraApp",
                                                        "Video saved to: $uri"
                                                    )
                                                }
                                            } else {
                                                // Stop recording
                                                stoneCameraViewModel.stopRecording()
                                            }
                                        }
                                    }

                                },
                                contentAlignment = Alignment.Center
                            ) {
                            }
                        }

                        // Camera Switcher Button
                        IconButton(
                            onClick = { stoneCameraViewModel.toggleCameraFacing() },
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
    }
}
