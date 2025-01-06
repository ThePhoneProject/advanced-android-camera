package co.stonephone.stonecamera.utils

import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView


fun getVisibleRange(previewView: PreviewView, imageCapture: ImageCapture): List<Float> {
    val previewViewWidth = previewView.width
    val previewViewHeight = previewView.height
    val previewViewAspectRatio = previewViewWidth.toFloat() / previewViewHeight.toFloat()

    val resolutionInfo = imageCapture.resolutionInfo?.resolution
    val visibleAspectRatio = resolutionInfo?.let {
        it.width.toFloat() / it.height.toFloat()
    } ?: 1f

    var topOfVisible = 0f
    var bottomOfVisible = 0f
    var leftOfVisible = 0f
    var rightOfVisible = 0f

    if (visibleAspectRatio < previewViewAspectRatio) {
        val newWidth = previewViewHeight / visibleAspectRatio
        leftOfVisible = (previewViewWidth - newWidth) / 2
        rightOfVisible = leftOfVisible + newWidth
        topOfVisible = 0f
        bottomOfVisible = previewViewHeight.toFloat()
    } else {
        val newHeight = previewViewWidth * visibleAspectRatio
        topOfVisible = (previewViewHeight - newHeight) / 2
        bottomOfVisible = topOfVisible + newHeight
        leftOfVisible = 0f
        rightOfVisible = previewViewWidth.toFloat()
    }

    return listOf(topOfVisible, bottomOfVisible, leftOfVisible, rightOfVisible)
}