package co.stonephone.stonecamera.utils

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import android.graphics.Rect
import co.stonephone.stonecamera.MyApplication

fun calculateImageCoverageRegion(
    previewView: PreviewView,
    imageCapture: ImageCapture,
): Rect? {
    val resolutionInfo = imageCapture.resolutionInfo?.resolution
    // Get the scale type of the PreviewView
    val scaleType = previewView.scaleType

    // Get the dimensions of the PreviewView
    val previewWidth = previewView.width.toFloat()
    val previewHeight = previewView.height.toFloat()

    // Extract the resolution from the camera
    val imageWidth = resolutionInfo?.width ?: 0
    val imageHeight = resolutionInfo?.height ?: 0

    if (imageWidth == 0 || imageHeight == 0 || previewWidth == 0f || previewHeight == 0f) {
        // Return a zero Rect if dimensions are invalid
        return Rect(0, 0, 0, 0)
    }

    // Calculate the aspect ratios
    val previewAspectRatio = previewWidth / previewHeight
    val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

    // Initialize the Rect for coverage area
    var left = 0f
    var top = 0f
    var right = previewWidth
    var bottom = previewHeight

    when (scaleType) {
        PreviewView.ScaleType.FILL_CENTER -> {
            if (imageAspectRatio > previewAspectRatio) {
                // Image is wider than the PreviewView; scale by height
                val scaledWidth = previewHeight * imageAspectRatio
                left = (previewWidth - scaledWidth) / 2
                right = left + scaledWidth
            } else {
                // Image is taller than the PreviewView; scale by width
                val scaledHeight = previewWidth / imageAspectRatio
                top = (previewHeight - scaledHeight) / 2
                bottom = top + scaledHeight
            }
        }

        PreviewView.ScaleType.FIT_CENTER -> {
            if (imageAspectRatio > previewAspectRatio) {
                // Image is wider than the PreviewView; scale by width
                val scaledHeight = previewWidth * imageAspectRatio
                top = (previewHeight - scaledHeight) / 2
                bottom = top + scaledHeight
            } else {
                // Image is taller than the PreviewView; scale by height
                val scaledWidth = previewHeight * imageAspectRatio
                left = (previewWidth - scaledWidth) / 2
                right = left + scaledWidth
            }
        }

        PreviewView.ScaleType.FILL_START -> {
            if (imageAspectRatio > previewAspectRatio) {
                // Image is wider than the PreviewView; scale by height
                val scaledWidth = previewHeight * imageAspectRatio
                right = scaledWidth
            } else {
                // Image is taller than the PreviewView; scale by width
                val scaledHeight = previewWidth / imageAspectRatio
                bottom = scaledHeight
            }
        }

        PreviewView.ScaleType.FILL_END -> {
            if (imageAspectRatio > previewAspectRatio) {
                // Image is wider than the PreviewView; scale by height
                val scaledWidth = previewHeight * imageAspectRatio
                left = previewWidth - scaledWidth
                right = previewWidth
            } else {
                // Image is taller than the PreviewView; scale by width
                val scaledHeight = previewWidth / imageAspectRatio
                top = previewHeight - scaledHeight
                bottom = previewHeight
            }
        }

        else -> {
            // Default to FILL_CENTER behavior
            return null
        }
    }


    val density = MyApplication.getAppContext().resources.displayMetrics.density
    return Rect(
        (left / density + 0.5f).toInt(),
        (top / density + 0.5f).toInt(),
        (right / density + 0.5f).toInt(),
        (bottom / density + 0.5f).toInt()
    )
}
