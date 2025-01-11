package co.stonephone.stonecamera.utils

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import kotlin.math.sqrt


/**
 * A simple data class to hold the key information.
 * Note that `lensFacing` is an Int? because it can be
 * null if the device doesn't report it, but typically
 * you'll get one of:
 *  - CameraCharacteristics.LENS_FACING_FRONT (0)
 *  - CameraCharacteristics.LENS_FACING_BACK (1)
 *  - CameraCharacteristics.LENS_FACING_EXTERNAL (2)
 */
data class StoneCameraInfo(
    val cameraId: String,
    val lensFacing: Int?,
    val sensorMp: Double,
    val cameraType: String,
    val minZoom: Float,
    val maxZoom: Float,
    val focalLength: Float?,
    var relativeZoom: Float? = null,
)


@SuppressLint("RestrictedApi")
public fun createCameraSelectorForId(targetCameraId: String): CameraSelector {
    return CameraSelector.Builder()
        // addCameraFilter() passes a list of available cameras. We'll filter by matching camera ID.
        .addCameraFilter { cameraInfos ->
            cameraInfos.filter { cameraInfo ->
                // Cast to CameraInfoInternal so we can retrieve the actual camera ID string
                val internalInfo = cameraInfo as? CameraInfoInternal
                // Compare with the desired camera ID
                internalInfo?.cameraId == targetCameraId
            }
        }
        .build()
}

/**
 * Returns a list of StoneCameraInfo for all camera devices on the system,
 * applying any device-specific overrides if present.
 */
fun getAllCamerasInfo(context: Context): List<StoneCameraInfo> {
    val cameraInfos = mutableListOf<StoneCameraInfo>()
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    try {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            // 1) Lens facing direction
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            // 2) Approximate sensor MP
            val pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            var mp = 0.0
            if (pixelArraySize != null) {
                mp = (pixelArraySize.width.toDouble() * pixelArraySize.height.toDouble()) / 1_000_000.0
            }

            // 3) Determine camera type by computing 35 mm-equivalent focal length
            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val physicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            var cameraType = "Unknown"
            var focalLength:Float? = null

            if (focalLengths != null && focalLengths.isNotEmpty() && physicalSize != null) {
                val focalLengthMm = focalLengths[0]
                val sensorDiagonalMm = sqrt(
                    physicalSize.width * physicalSize.width +
                            physicalSize.height * physicalSize.height
                )
                val fullFrameDiagonal = 43.27f // 35mm "full frame" diagonal in mm
                val eqFocalLength = focalLengthMm * (fullFrameDiagonal / sensorDiagonalMm)
                focalLength = eqFocalLength
                cameraType = classifyCameraType(eqFocalLength)
            }

            // 4) Zoom range
            val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            val minZoom = 1.0f
            val maxZoom = maxDigitalZoom

            // Construct the StoneCameraInfo from real data.
            val info = StoneCameraInfo(
                cameraId = cameraId,
                lensFacing = lensFacing,
                sensorMp = mp,
                cameraType = cameraType,
                minZoom = minZoom,
                maxZoom = maxZoom,
                focalLength = focalLength,
            )

            cameraInfos.add(info)
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }

    return getRelativeZoomLevels(cameraInfos)
}

/**
 * Classify the camera type based on an approximate 35mm-equivalent focal length.
 * Feel free to tweak these thresholds for your particular definition of ultrawide / wide / tele.
 */
private fun classifyCameraType(eqFocalLength: Float): String {
    return when {
        eqFocalLength < 18f -> "Ultrawide"
        eqFocalLength < 30f -> "Wide"
        else -> "Telephoto"
    }
}



private fun getRelativeZoomLevels(infos: List<StoneCameraInfo>):List<StoneCameraInfo> {
    // For front and then for back
    // Find primary camera (the highest MP "wide" lens, otherwise the next highest MP)
    // Calculate other cameras relative zoom based on focal length
    // For display, round to 1dp

    val frontCameras = infos.filter { it.lensFacing == CameraCharacteristics.LENS_FACING_FRONT }
    val backCameras = infos.filter { it.lensFacing == CameraCharacteristics.LENS_FACING_BACK }
    
    val wideFrontCameras = frontCameras.filter { it.cameraType == "Wide" }
    val primaryFront = frontCameras.maxByOrNull { it.sensorMp } ?: wideFrontCameras.maxByOrNull { it.sensorMp }

    val wideBackCameras = backCameras.filter { it.cameraType == "Wide" }
    val primaryBack = backCameras.maxByOrNull { it.sensorMp } ?: wideBackCameras.maxByOrNull { it.sensorMp }

    val primaryFrontFocalLength = primaryFront?.focalLength ?: 0f
    val primaryBackFocalLength = primaryBack?.focalLength ?: 0f

    for (camera in infos) {
        val relativeZoom = when (camera.lensFacing) {
            CameraCharacteristics.LENS_FACING_FRONT -> {
                (camera.focalLength ?: 0f) / primaryFrontFocalLength
            }
            CameraCharacteristics.LENS_FACING_BACK -> {
                (camera.focalLength ?: 0f) / primaryBackFocalLength
            }
            else -> 1f
        }
        camera.relativeZoom = relativeZoom
    }

    return infos
}
