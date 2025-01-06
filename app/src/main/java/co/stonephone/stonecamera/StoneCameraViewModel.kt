// StoneCameraViewModel.kt
package co.stonephone.stonecamera

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import co.stonephone.stonecamera.utils.StoneCameraInfo
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

class StoneCameraViewModel : ViewModel() {

    private val ZOOM_CANCEL_THRESHOLD = 0.1f

    //--------------------------------------------------------------------------------
    // Core CameraX use-cases (built once and shared)
    //--------------------------------------------------------------------------------
    val imageCapture: ImageCapture = ImageCapture.Builder().build()

    val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD))
        .build()

    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)

    //--------------------------------------------------------------------------------
    // Mutable state that drives the UI
    //--------------------------------------------------------------------------------
    var camera: Camera? by mutableStateOf(null)
        private set

    var selectedCameraId by mutableStateOf("0")
        private set

    var facing by mutableStateOf(CameraSelector.LENS_FACING_BACK)
        private set

    var relativeZoomFactor by mutableStateOf(1f)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var selectedMode by mutableStateOf("Photo")
        private set

    // TODO: make it save between sessions
    var showShutterFlash by mutableStateOf(false)
        private set

    var focusPoint by mutableStateOf<Pair<Float, Float>?>(null)
        private set

    // This list is loaded/updated externally (from the composable) once we have a context, 
    // or you can do it in the init block if you don’t need context changes.
    var cameras: List<StoneCameraInfo> by mutableStateOf(emptyList())
        private set

    // The filtered list of cameras (facing) 
    // Update whenever `facing` changes or `cameras` changes
    var facingCameras by mutableStateOf(emptyList<StoneCameraInfo>())
        private set

    //--------------------------------------------------------------------------------
    // Public methods to manipulate the above states
    //--------------------------------------------------------------------------------

    fun loadCameras(allCameras: List<StoneCameraInfo>) {
        cameras = allCameras
        // Update facingCameras to match the current 'facing'
        updateFacingCameras()
    }

    fun toggleCameraFacing() {
        cancelFocus("camera flipped")

        val newFacing = if (facing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        facing = newFacing
        updateFacingCameras()
    }

    private fun updateFacingCameras() {
        facingCameras = cameras.filter { it.lensFacing == facing }
        // Optionally reset the cameraId and zoom
        if (facingCameras.isNotEmpty()) {
            // pick the default (zoom=1.0)
            val (newCam, _) = selectCameraForStepZoomLevel(1f, facingCameras)
            selectedCameraId = newCam.cameraId
            setZoom(1f) // reset zoom
        }
    }

    /**
     * Called when the camera is connected. We store the reference in ViewModel.
     */
    fun onCameraConnected(camera: Camera) {
        this.camera = camera
    }

    /**
     * Switch between Photo and Video modes.
     */
    fun selectMode(mode: String) {
        // If we switch away from Video while recording, we stop
        if (mode == "Photo" && isRecording) {
            stopRecording()
        }
        selectedMode = mode
    }

    /**
     * Called from ZoomBar or whenever we want to change the user zoom factor.
     */
    fun setZoom(zoomFactor: Float) {
        val oldZoom = relativeZoomFactor
//        relativeZoomFactor = zoomFactor

        // If difference in zoom is above threshold, we auto-cancel focus
        if (kotlin.math.abs(zoomFactor - oldZoom) > ZOOM_CANCEL_THRESHOLD) {
            cancelFocus("zoom changed by more than $ZOOM_CANCEL_THRESHOLD")
        }

        val maxCamera = cameras.maxByOrNull { it.relativeZoom ?: 1f } ?: return
        val maxRelativeZoom = (maxCamera.relativeZoom ?: 1f) * maxCamera.maxZoom
        val minRelativeZoom = cameras.minByOrNull { it.relativeZoom ?: 1f }?.relativeZoom ?: 1.0f

        val newRelativeZoom = zoomFactor.coerceIn(minRelativeZoom, maxRelativeZoom)
        relativeZoomFactor = newRelativeZoom

        val (targetCamera, actualZoomRatio) = selectCameraForStepZoomLevel(newRelativeZoom, facingCameras)
        if (targetCamera.cameraId != selectedCameraId) {
            selectedCameraId = targetCamera.cameraId
        }

        camera?.cameraControl?.setZoomRatio(actualZoomRatio)
    }

    /**
     * For showing/hiding a shutter flash overlay when capturing a photo.
     */
    fun triggerShutterFlash() {
        showShutterFlash = true
    }

    fun onShutterFlashComplete() {
        showShutterFlash = false
    }

    /**
     * Setting focus point (for tap-to-focus).
     */
    fun setFocusPoint(x: Float, y: Float) {
        focusPoint = x to y
    }

    fun cancelFocus(reason: String? = null) {
        Log.d("StoneCameraViewModel", "Cancel focus because: $reason")
        // Remove the reticle from UI
        focusPoint = null
        setBrightness(0f)
        // Tell CameraX to cancel the focus and metering regions
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    fun capturePhoto(imageCapture: ImageCapture) {
        StoneCameraAppHelpers.capturePhoto(imageCapture)
    }

    //--------------------------------------------------------------------------------
    // Recording logic
    //--------------------------------------------------------------------------------

    private var currentRecording: Recording? = null

    fun startRecording(
        videoCapture: VideoCapture<Recorder>,
        onVideoSaved: (Uri) -> Unit
    ) {
        isRecording = true
        currentRecording = StoneCameraAppHelpers.startRecording(
            videoCapture = videoCapture,
            onVideoSaved = { uri ->
                isRecording = false
                onVideoSaved(uri)
            }
        )
    }

    fun stopRecording() {
        currentRecording?.stop()
        isRecording = false
    }

    /**
     * Adjust the brightness of the camera preview and captured images.
     * @param brightnessLevel A value between -1.0 (darkest) and 1.0 (brightest).
     */
    fun setBrightness(brightnessLevel: Float) {
        val clampedBrightness = brightnessLevel.coerceIn(-1.0f, 1.0f)
        camera?.cameraControl?.setExposureCompensationIndex(
            calculateExposureCompensationIndex(clampedBrightness)
        )
    }

    /**
     * Helper to map brightness level (-1.0 to 1.0) to CameraX exposure compensation index.
     */
    private fun calculateExposureCompensationIndex(brightnessLevel: Float): Int {
        val exposureRange = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return 0
        val maxIndex = exposureRange.upper
        val minIndex = exposureRange.lower

        // Map brightness level to exposure index range
        return ((brightnessLevel + 1.0f) / 2.0f * (maxIndex - minIndex) + minIndex).toInt()
    }

    //--------------------------------------------------------------------------------
    // Mutable state for Flash Mode
    //--------------------------------------------------------------------------------
    var flashMode by mutableStateOf("OFF")
        private set

    /**
     * Set the flash mode for the camera.
     * Supported modes: ON, OFF, AUTO.
     * @param mode A string representing the flash mode ("ON", "OFF", "AUTO").
     */
    fun setFlash(mode: String) {
        val newFlashMode = when (mode.uppercase()) {
            "ON" -> ImageCapture.FLASH_MODE_ON
            "OFF" -> ImageCapture.FLASH_MODE_OFF
            "AUTO" -> ImageCapture.FLASH_MODE_AUTO
            else -> {
                Log.e("StoneCameraViewModel", "Invalid flash mode: $mode. Defaulting to OFF.")
                ImageCapture.FLASH_MODE_OFF
            }
        }

        imageCapture.flashMode = newFlashMode
        flashMode = mode.uppercase() // Update the state
    }
}
