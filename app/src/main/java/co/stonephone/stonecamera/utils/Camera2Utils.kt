package co.stonephone.stonecamera.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import kotlin.math.abs

@OptIn(ExperimentalCamera2Interop::class)
fun getLargestSensorSize(camera: Camera, context: Context): Size? {
    // Access Camera2CameraInfo from the CameraX camera
    val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
    val cameraId = camera2CameraInfo.cameraId // Retrieve the Camera ID

    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val chars = manager.getCameraCharacteristics(cameraId)
    val config = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
    val sizes = config.getOutputSizes(ImageFormat.JPEG) ?: return null
    return sizes.maxByOrNull { it.width.toLong() * it.height.toLong() }
}

@OptIn(ExperimentalCamera2Interop::class)
fun getLargestMatchingSize(
    camera: Camera,
    context: Context,
    requestedRatio: Float
): Size? {
    // Access Camera2CameraInfo from the CameraX camera
    val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
    val cameraId = camera2CameraInfo.cameraId // Retrieve the Camera ID

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
