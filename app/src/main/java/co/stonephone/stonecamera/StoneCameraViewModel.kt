// StoneCameraViewModel.kt
package co.stonephone.stonecamera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.stonephone.stonecamera.utils.PrefStateDelegate
import co.stonephone.stonecamera.utils.StoneCameraInfo
import co.stonephone.stonecamera.utils.getLargestMatchingSize
import co.stonephone.stonecamera.utils.getLargestSensorSize
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

class StoneCameraViewModel(private val context: Context) : ViewModel() {

    private val ZOOM_CANCEL_THRESHOLD = 0.1f

    val supportedAspectRatios = listOf("4:3", "16:9", "FULL")

    var selectedAspectRatio by PrefStateDelegate(context, "aspect_ratio", "16:9")
        private set

    var flashMode by PrefStateDelegate(context, "flash_mode", "OFF")
        private set

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
    // Core CameraX use-cases (built once and shared)
    //--------------------------------------------------------------------------------
    var preview: Preview = createPreview(selectedAspectRatio)
        private set

    var imageCapture: ImageCapture = createImageCapture(selectedAspectRatio)

    val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD))
        .build()

    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)


    //--------------------------------------------------------------------------------
    // Init
    //--------------------------------------------------------------------------------
    init {
        // Rebuild use-cases to match our persisted prefs
        recreateUseCases()
    }


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

        val (targetCamera, actualZoomRatio) = selectCameraForStepZoomLevel(
            newRelativeZoom,
            facingCameras
        )
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

    /**
     * Build a ResolutionSelector that "prefers" [targetSize] if not null,
     * otherwise tries to fallback to an aspect ratio if [ratio] is non-null,
     * or does default if both are null.
     */
    private fun buildResolutionSelector(
        targetSize: Size?,     // e.g. 3000×3000 for 1:1
        ratio: Float?          // e.g. 1.0f for 1:1, 1.333...f for 4:3, etc.
    ): ResolutionSelector {
        val aspectRatioConst = when {
            ratio == null -> null // “FULL” or unknown
            kotlin.math.abs(ratio - (4f / 3f)) < 0.01f -> AspectRatio.RATIO_4_3
            kotlin.math.abs(ratio - (16f / 9f)) < 0.01f -> AspectRatio.RATIO_16_9
            else -> null // e.g. 1:1 or any custom ratio
        }

        // If no targetSize is found, rely entirely on aspectRatioConst or a fallback
        if (targetSize == null) {
            // No recognized ratio => default to RATIO_4_3 fallback
            return if (aspectRatioConst == null) {
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_4_3,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            } else {
                // Use the recognized built-in ratio
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            aspectRatioConst,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            }
        }

        // We do have a targetSize (the largest size matching the custom ratio, or “FULL” sensor size).
        val resolutionStrategy = ResolutionStrategy(
            targetSize,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )

        // If ratio maps to 4:3 or 16:9, we can also supply an AspectRatioStrategy fallback
        return if (aspectRatioConst != null) {
            ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        aspectRatioConst,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()
        } else {
            // ratio is something else (e.g. 1:1), so no built-in aspect ratio
            // rely on the resolutionStrategy alone
            ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .build()
        }
    }


    //--------------------------------------------------------------------------------
    // Flash
    //--------------------------------------------------------------------------------
    fun setFlash(mode: String) {
        val newFlashMode = flashModeStringToMode(mode)
        flashMode = mode.uppercase()
        imageCapture.flashMode = newFlashMode
    }

    private fun flashModeStringToMode(modeStr: String): Int {
        return when (modeStr.uppercase()) {
            "ON" -> ImageCapture.FLASH_MODE_ON
            "OFF" -> ImageCapture.FLASH_MODE_OFF
            "AUTO" -> ImageCapture.FLASH_MODE_AUTO
            else -> {
                Log.e("StoneCameraViewModel", "Invalid flash mode: $modeStr. Defaulting to OFF.")
                ImageCapture.FLASH_MODE_OFF
            }
        }
    }

    fun setAspectRatio(ratio: String) {
        if (ratio !in supportedAspectRatios) return
        selectedAspectRatio = ratio
        recreateUseCases()
    }

    private fun recreateUseCases() {
        preview = createPreview(selectedAspectRatio)
        imageCapture = createImageCapture(selectedAspectRatio).also {
            it.flashMode = flashModeStringToMode(flashMode)
        }
    }

    //--------------------------------------------------------------------------------
    // Building with ResolutionSelector
    //--------------------------------------------------------------------------------

    private fun parseRatioOrNull(ratioStr: String): Float? {
        val nums = ratioStr.split(":").map { it.toFloatOrNull() }
        if(nums.any { it == null }) return null
        return nums[0]!! / nums[1]!!
    }

    private fun createPreview(ratioStr: String): Preview {
        val ratioOrNull = parseRatioOrNull(ratioStr)
        val cameraId = selectedCameraId.ifEmpty { "0" }

        // 1) Figure out the best "preferred size" for this ratio
        val targetSize = if (ratioOrNull == null) {
            getLargestSensorSize(cameraId, context)
        } else {
            getLargestMatchingSize(cameraId, context, ratioOrNull)
        }

        // 2) Build a ResolutionSelector
        val resolutionSelector = buildResolutionSelector(targetSize, ratioOrNull)

        // 3) Apply to the Preview builder
        return Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                Log.d("StoneCameraViewModel", "Preview => ratio=$ratioStr, size=$targetSize")
            }
    }

    private fun createImageCapture(ratioStr: String): ImageCapture {
        val ratioOrNull = parseRatioOrNull(ratioStr)
        val cameraId = selectedCameraId.ifEmpty { "0" }

        val targetSize = if (ratioOrNull == null) {
            getLargestSensorSize(cameraId, context)
        } else {
            getLargestMatchingSize(cameraId, context, ratioOrNull)
        }

        val resolutionSelector = buildResolutionSelector(targetSize, ratioOrNull)

        return ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                Log.d("StoneCameraViewModel", "ImageCapture => ratio=$ratioStr, size=$targetSize")
            }
    }
}

class StoneCameraViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoneCameraViewModel::class.java)) {
            return StoneCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}