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
    modifier: Modifier = Modifier,
    onPreviewView: (PreviewView) -> Unit,
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = {
                val previewView = PreviewView(it)
                onPreviewView(previewView)
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
        )
    }
}
