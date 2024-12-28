package co.stonephone.stonecamera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A composable that creates a PreviewView for camera output.
 * Once it's created, calls [onPreviewViewCreated] so the parent
 * can bind the camera use cases.
 */
@Composable
fun StoneCameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val previewView = PreviewView(context).apply {
                // Optional scale type:
                 scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            // Notify the parent that the PreviewView is ready.
            onPreviewViewCreated(previewView)
            previewView
        }
    )
}
