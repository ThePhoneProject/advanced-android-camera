package co.stonephone.stonecamera.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import kotlin.math.abs

fun getLargestSensorSize(cameraId: String, context: Context): Size? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val chars = manager.getCameraCharacteristics(cameraId)
    val config = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
    val sizes = config.getOutputSizes(ImageFormat.JPEG) ?: return null
    return sizes.maxByOrNull { it.width.toLong() * it.height.toLong() }
}

fun getLargestMatchingSize(
    cameraId: String,
    context: Context,
    requestedRatio: Float
): Size? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val chars = manager.getCameraCharacteristics(cameraId)
    val config = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
    val sizes = config.getOutputSizes(ImageFormat.JPEG) ?: return null

    var bestSize: Size? = null
    var bestRatioDiff = Float.MAX_VALUE
    var bestArea = 0L

    for (size in sizes) {
        val ratio = size.width.toFloat() / size.height.toFloat()
        val diff = abs(ratio - requestedRatio)
        if (diff < bestRatioDiff) {
            bestRatioDiff = diff
            bestSize = size
            bestArea = size.width.toLong() * size.height.toLong()
        } else if (abs(diff - bestRatioDiff) < 1e-3) {
            // Ratios are equally close, pick the larger area
            val area = size.width.toLong() * size.height.toLong()
            if (area > bestArea) {
                bestSize = size
                bestArea = area
            }
        }
    }
    return bestSize
}
