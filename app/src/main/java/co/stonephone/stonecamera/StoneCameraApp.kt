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
import co.stonephone.stonecamera.ui.FocusReticle
import co.stonephone.stonecamera.ui.ShutterFlashOverlay
import co.stonephone.stonecamera.ui.StoneCameraPreview
import co.stonephone.stonecamera.ui.ZoomBar
import co.stonephone.stonecamera.utils.getAllCamerasInfo
import co.stonephone.stonecamera.utils.getVisibleRange

val shootModes = arrayOf("Photo", "Video")

@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun StoneCameraApp(
    cameraProvider: ProcessCameraProvider,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val stoneCameraViewModel = viewModel<StoneCameraViewModel>(
        factory = StoneCameraViewModelFactory(context)
    )

    // Observe states from the ViewModel
    val camera = stoneCameraViewModel.camera
    val imageCapture by remember { stoneCameraViewModel::imageCapture }
    val videoCapture = stoneCameraViewModel.videoCapture
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val selectedCameraId = stoneCameraViewModel.selectedCameraId
    val facing = stoneCameraViewModel.facing
    val relativeZoomFactor by remember { stoneCameraViewModel::relativeZoomFactor }
    val isRecording = stoneCameraViewModel.isRecording
    val selectedMode = stoneCameraViewModel.selectedMode
    val showShutterFlash = stoneCameraViewModel.showShutterFlash
    val focusPoint = stoneCameraViewModel.focusPoint
    val flashMode = stoneCameraViewModel.flashMode
    val preview = stoneCameraViewModel.preview
    val aspectRatio = stoneCameraViewModel.selectedAspectRatio
    var visibleDimensions: List<Float>? by remember { mutableStateOf(null) }

    // We can load cameras once (or whenever context changes) and pass them to the ViewModel
    LaunchedEffect(Unit) {
        val allCameras = getAllCamerasInfo(context)
        stoneCameraViewModel.loadCameras(allCameras)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview behind everything
        StoneCameraPreview(
            cameraProvider = cameraProvider,
            selectedCameraId = selectedCameraId,
            lifecycleOwner = lifecycleOwner,
            imageCapture = imageCapture,
            videoCapture = videoCapture,
            preview = preview,
            stoneCameraViewModel = stoneCameraViewModel,
            onPreviewViewConnected = { pView, cam ->
                var isScaling = false
                previewView = pView
                visibleDimensions = getVisibleRange(previewView!!, imageCapture)
                stoneCameraViewModel.onCameraConnected(cam)

                // Set up touch gestures for focus and zoom
                var scaleGestureDetector: ScaleGestureDetector? = null

                previewView!!.setOnTouchListener { view, event ->
                    scaleGestureDetector = scaleGestureDetector ?: ScaleGestureDetector(context,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                isScaling = true
                                return super.onScaleBegin(detector)
                            }

                            override fun onScaleEnd(detector: ScaleGestureDetector) {
                                isScaling = false
                                return super.onScaleEnd(detector)
                            }

                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val zoomChange = detector.scaleFactor
                                val newZoom = relativeZoomFactor * zoomChange
                                stoneCameraViewModel.setZoom(newZoom)
                                return true
                            }
                        }
                    )

                    scaleGestureDetector?.onTouchEvent(event)

                    if (!isScaling && event.action == android.view.MotionEvent.ACTION_UP) {
                        val x = event.x
                        val y = event.y

                        val (topOfVisible, bottomOfVisible, leftOfVisible, rightOfVisible) = visibleDimensions!!
                        val isWithinVisible =
                            x >= leftOfVisible && x <= rightOfVisible && y >= topOfVisible && y <= bottomOfVisible

                        if (!isWithinVisible) {
                            stoneCameraViewModel.cancelFocus("focus point outside of visible area")
                            return@setOnTouchListener false
                        }

                        // Get the metering point factory
                        val factory = previewView!!.meteringPointFactory
                        val meteringPoint = factory.createPoint(x, y)

                        // Trigger focus at the metering point
                        val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
                        cam.cameraControl.startFocusAndMetering(focusAction)

                        // Update the ViewModel with the new focus point
                        stoneCameraViewModel.setFocusPoint(x, y)
                    }
                    true
                }

            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val nextMode = when (flashMode) {
                        "OFF" -> "ON"
                        "ON" -> "AUTO"
                        "AUTO" -> "OFF"
                        else -> {
                            "OFF"
                        }
                    }
                    stoneCameraViewModel.setFlash(nextMode)
                }
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        "OFF" -> Icons.Default.FlashOff // Replace with your preferred icon
                        "ON" -> Icons.Default.FlashOn
                        "AUTO" -> Icons.Default.FlashAuto // You may need to add custom icons for Flash Auto
                        else -> {
                            Icons.Default.FlashOff
                        }
                    },
                    contentDescription = when (flashMode) {
                        "OFF" -> "Flash Off"
                        "ON" -> "Flash On"
                        "AUTO" -> "Flash Auto"
                        else -> {
                            "Flash"
                        }
                    },
                    tint = Color.White // Customize as needed
                )
            }

            Text(
                text = aspectRatio,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
                    .clickable(onClick = {
                        val nextAspectRatio = when (aspectRatio) {
                            "16:9" -> "4:3"
                            "4:3" -> "FULL"
                            else -> "16:9"
                        }
                        stoneCameraViewModel.setAspectRatio(nextAspectRatio)
                    })
            )
        }

        // Show focus reticle if set
        focusPoint?.let { (x, y) ->
            FocusReticle(x, y, onDismissFocus = {
                stoneCameraViewModel.cancelFocus("focus reticle dismissed")
            }, onSetBrightness = { brightness ->
                stoneCameraViewModel.setBrightness(brightness)
            }, visibleDimensions = visibleDimensions!!, context = context)
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
            // Zoom controls, only if the camera is set
            camera?.let {
                ZoomBar(
                    camera = it,
                    cameraProvider = cameraProvider,
                    relativeZoom = relativeZoomFactor,
                    setZoom = { zoomFactor -> stoneCameraViewModel.setZoom(zoomFactor) },
                    context = context,
                    cameras = stoneCameraViewModel.facingCameras
                )
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
