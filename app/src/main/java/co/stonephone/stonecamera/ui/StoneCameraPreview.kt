package co.stonephone.stonecamera.ui

import android.annotation.SuppressLint
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
