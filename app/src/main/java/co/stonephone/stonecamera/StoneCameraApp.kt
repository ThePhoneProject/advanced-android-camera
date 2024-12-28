package co.stonephone.stonecamera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import co.stonephone.stonecamera.ui.ShutterFlashOverlay
import co.stonephone.stonecamera.ui.StoneCameraPreview
import kotlinx.coroutines.delay
import java.io.File

val shootModes = arrayOf("Photo", "Video")

private var camera:Camera? = null
private var previewView: PreviewView? = null

@SuppressLint("ClickableViewAccessibility")
@Composable
fun StoneCameraApp(
    cameraProvider: ProcessCameraProvider
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // --- Camera use cases ---
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
    }
    val videoCapture = remember {
        VideoCapture.withOutput(recorder)
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Track if we're recording video
    var isRecording by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Photo") }

    // NEW: Track whether to show a shutter flash overlay
    var showShutterFlash by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera preview behind everything
        StoneCameraPreview(
            onPreviewViewCreated = { pView ->
                previewView = pView
                camera = bindAllCameraUseCases(
                    cameraProvider = cameraProvider,
                    cameraSelector = cameraSelector,
                    previewView = previewView!!,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture,
                    lifecycleOwner = lifecycleOwner
                )

                previewView!!.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        val x = event.x
                        val y = event.y

                        // Use MeteringPointFactory for focus
                        val factory = previewView!!.meteringPointFactory
                        val meteringPoint = factory.createPoint(x, y)

                        // Trigger focus at the metering point
                        val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
                        camera?.cameraControl?.startFocusAndMetering(focusAction)

                        // Map touch point for visual feedback
                        val scaledX = x / previewView!!.width * previewView!!.width
                        val scaledY = y / previewView!!.height * previewView!!.height
                        focusPoint = scaledX to scaledY
                    }
                    true
                }

            }
        )

        focusPoint?.let { (x, y) ->
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(x.dp - 25.dp, y.dp - 25.dp) // Center the circle
                    .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
            )
            LaunchedEffect(focusPoint) {
                delay(500) // 500ms
                focusPoint = null
            }
        }

        // If showShutterFlash is true, display the overlay
        if (showShutterFlash) {
            ShutterFlashOverlay(onFlashComplete = {
                showShutterFlash = false
            })
        }

        // Translucent overlay for mode switch & shutter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    shootModes.forEach { mode ->
                        Text(
                            text = mode,
                            color = if (mode == selectedMode) Color.White else Color.Gray,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable {
                                selectedMode = mode
                                // if we switch from Video to Photo while recording, stop the recording
                                if (selectedMode == "Photo" && isRecording) {
                                    stopRecording(videoCapture)
                                    isRecording = false
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Switcher Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Gray, shape = CircleShape)
                            .clickable {
                                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                camera = bindAllCameraUseCases(
                                    cameraProvider = cameraProvider,
                                    cameraSelector = cameraSelector,
                                    previewView = previewView!!,
                                    imageCapture = imageCapture,
                                    videoCapture = videoCapture,
                                    lifecycleOwner = lifecycleOwner
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Flip",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.9f), shape = CircleShape)
                            .clickable {
                                when (selectedMode) {
                                    "Photo" -> {
                                        // Trigger the shutter flash overlay
                                        showShutterFlash = true
                                        // Then capture the photo
                                        capturePhoto(imageCapture, context)
                                    }
                                    "Video" -> {
                                        if (!isRecording) {
                                            startRecording(videoCapture, context) {
                                                Log.d("StoneCameraApp", "Video saved to: $it")
                                            }
                                            isRecording = true
                                        } else {
                                            stopRecording(videoCapture)
                                            isRecording = false
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedMode == "Video" && isRecording) "Stop" else "Shoot",
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------
// Helper functions for binding use cases & capturing
// ------------------------------------------------------

private fun bindAllCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    cameraSelector: CameraSelector,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    lifecycleOwner: LifecycleOwner
) :Camera? {
    try {
        cameraProvider.unbindAll()

        // Build a Preview use case & set its surface
        val previewUseCase = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        // Bind all use cases
        return cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            previewUseCase,
            imageCapture,
            videoCapture
        )
    } catch (e: Exception) {
        Log.e("StoneCameraApp", "Binding use cases failed", e)
    }
    return null
}

/**
 * Simple photo capture logic:
 *  - create a file
 *  - call takePicture
 */
// In StoneCameraApp.kt (or wherever you keep your logic):
private fun capturePhoto(
    imageCapture: ImageCapture,
    context: android.content.Context
) {
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/StoneCameraApp")
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                Log.d("StoneCameraApp", "Photo saved at: $savedUri")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("StoneCameraApp", "Photo capture failed: $exception")
            }
        }
    )
}

private var currentRecording: Recording? = null

@SuppressLint("MissingPermission")
private fun startRecording(
    videoCapture: VideoCapture<Recorder>,
    context: android.content.Context,
    onVideoSaved: (Uri) -> Unit
) {
    val filename = "VID_${System.currentTimeMillis()}.mp4"

    // Metadata for MediaStore
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/StoneCameraApp")
    }

    // Use MediaStoreOutputOptions for video saving
    val outputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    )
        .setContentValues(contentValues)
        .build()

    // Start recording
    currentRecording = videoCapture.output
        .prepareRecording(context, outputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Log.d("StoneCameraApp", "Video recording started")
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val savedUri = recordEvent.outputResults.outputUri
                        onVideoSaved(savedUri)
                        Log.d("StoneCameraApp", "Video saved at: $savedUri")
                    } else {
                        Log.e(
                            "StoneCameraApp",
                            "Video recording error: ${recordEvent.error}"
                        )
                    }
                }
            }
        }
}

private fun stopRecording(videoCapture: VideoCapture<Recorder>) {
    currentRecording?.stop()
    Log.d("StoneCameraApp", "Stopping video recording")
}
