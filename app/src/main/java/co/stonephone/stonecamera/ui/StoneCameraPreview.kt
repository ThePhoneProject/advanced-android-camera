package co.stonephone.stonecamera.ui

import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import co.stonephone.stonecamera.StoneCameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
@Composable
fun StoneCameraPreview(
    cameraProvider: ProcessCameraProvider,
    selectedCameraId: String,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
    onPreviewViewConnected: (PreviewView, Camera) -> Unit,
    preview: Preview,
    stoneCameraViewModel: StoneCameraViewModel
) {
    val context = LocalContext.current
    var previewView by remember { mutableStateOf(PreviewView(context)) }

    val bindingJob = remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(cameraProvider, selectedCameraId, imageCapture, videoCapture, preview) {
        // Cancel the ongoing binding job, if any
        bindingJob.value?.cancel()

        // Launch a coroutine to bind the camera
        bindingJob.value = launch(Dispatchers.Main) {
            try {
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.Builder()
                    .addCameraFilter { cameraInfos ->
                        cameraInfos.filter { cameraInfo ->
                            // Cast to CameraInfoInternal so we can retrieve the actual camera ID string
                            val internalInfo = cameraInfo as? CameraInfoInternal
                            // Compare with the desired camera ID
                            internalInfo?.cameraId == selectedCameraId
                        }
                    }
                    .build()

                cameraProvider.unbindAll()

                // Bind the use cases to the camera
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

                if (stoneCameraViewModel.selectedAspectRatio === "FULL") {
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                } else {
                    previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
                }
                // Notify the caller that the new PreviewView is connected
                onPreviewViewConnected(previewView, camera)


            } catch (e: Exception) {
                // Handle binding errors
                e.printStackTrace()
            }
        }
    }

    Box(modifier = modifier) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
        )

    }
}
