// StoneCameraViewModel.kt
package co.stonephone.stonecamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.stonephone.stonecamera.plugins.IPlugin
import co.stonephone.stonecamera.plugins.PluginSetting
import co.stonephone.stonecamera.utils.StoneCameraInfo
import co.stonephone.stonecamera.utils.createCameraSelectorForId
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Suppress("UNCHECKED_CAST")
class StoneCameraViewModel(
    context: Context,
    private val registeredPlugins: List<IPlugin>
) :
    ViewModel() {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stone_camera_prefs", Context.MODE_PRIVATE)

    private val cameraExecutor = Executors.newSingleThreadExecutor()


    //--------------------------------------------------------------------------------
    // Mutable state that drives the UI
    //--------------------------------------------------------------------------------
    private var _previewView: PreviewView? by mutableStateOf(null)
    val previewView: PreviewView? get() = _previewView

    private var _cameraProvider: ProcessCameraProvider? = null
    val cameraProvider: ProcessCameraProvider? get() = _cameraProvider

    private var lifecycleOwner: LifecycleOwner? = null

    var camera: Camera? by mutableStateOf(null)
        private set

    private var _selectedCameraId = "0"

    var facing by mutableStateOf(CameraSelector.LENS_FACING_BACK)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var selectedMode by mutableStateOf("Photo")
        private set

    // TODO: make it save between sessions
    var showShutterFlash by mutableStateOf(false)
        private set

    private val _plugins = mutableListOf<IPlugin>()
    val plugins: List<IPlugin> get() = _plugins

    var pluginSettings: List<PluginSetting> = mutableListOf()

    // Mutable map to hold observable settings
    var settings: SnapshotStateMap<String, Any?> = mutableStateMapOf()

    private val previewViewTouchHandlers = mutableListOf<(MotionEvent) -> Boolean>()

    fun registerTouchHandler(handler: (MotionEvent) -> Boolean) {
        previewViewTouchHandlers.add(handler)
    }

    fun unregisterTouchHandler(handler: (MotionEvent) -> Boolean) {
        previewViewTouchHandlers.remove(handler)
    }


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
    var preview: Preview = createPreview()

    var imageCapture: ImageCapture = createImageCapture()

    var imageAnalysis: ImageAnalysis = createImageAnalysis()

    val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD))
        .build()

    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)


    //--------------------------------------------------------------------------------
    // Init
    //--------------------------------------------------------------------------------
    init {

        // Some settings affect use-cases, e.g. aspect ratio
        pluginSettings = registeredPlugins.flatMap { it.settings }
        registeredPlugins.forEach {
            it.settings.forEach { setting ->
                settings[setting.key] = getPluginSetting(setting.key)
            }
        }

        // Rebuild use-cases to match our persisted prefs
        recreateUseCases()
    }


    //--------------------------------------------------------------------------------
    // Public methods to manipulate the above states
    //--------------------------------------------------------------------------------

    fun onCameraProvider(provider: ProcessCameraProvider) {
        _cameraProvider = provider
        bindUseCases()
    }

    fun onLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        bindUseCases()
    }

    fun onPreviewView(view: PreviewView) {
        _previewView = view
        bindUseCases()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializePlugins() {
        _plugins.clear() // Clear any previously initialized plugins

        previewView?.setOnTouchListener { _, event ->
            previewViewTouchHandlers.forEach { handler ->
                handler(event)
            }
            true // If no handler consumes the event
        }

        registeredPlugins.forEach { plugin ->
            plugin.initialize(this) // Initialize the plugin
            _plugins.add(plugin) // Add to the initialized plugins list
        }


        pluginSettings = registeredPlugins.flatMap { it.settings }
        registeredPlugins.forEach {
            it.settings.forEach { setting ->
                settings[setting.key] = getPluginSetting(setting.key)
            }
        }
    }

    fun <T> getPluginSetting(settingKey: String): T? {
        val setting = pluginSettings.find { it.key == settingKey } ?: return null

        val defaultValue = setting.defaultValue

        @Suppress("UNCHECKED_CAST")
        return when (defaultValue) {
            is String -> prefs.getString(settingKey, defaultValue) as? T
            is Float -> prefs.getFloat(settingKey, defaultValue) as? T
            else -> null
        }
    }

    // Retrieve a setting with automatic recomposition support
    fun <T> getSetting(settingKey: String): T? {
        @Suppress("UNCHECKED_CAST")
        return settings[settingKey] as? T
    }

    // Update a setting and notify observers
    fun setSetting(settingKey: String, value: Any?) {
        val setting = pluginSettings.find { it.key == settingKey } ?: return

        when (setting) {
            is PluginSetting.EnumSetting -> {
                prefs.edit().putString(settingKey, value as String).apply()
            }

            is PluginSetting.ScalarSetting -> {
                prefs.edit().putFloat(settingKey, value as Float).apply()
            }

            is PluginSetting.CustomSetting -> {
                prefs.edit().putString(settingKey, value as String).apply()
            }
        }

        // Update the observable state map
        settings[settingKey] = value

        setting.onChange(this, value)
    }


    fun loadCameras(allCameras: List<StoneCameraInfo>) {
        cameras = allCameras
        // Update facingCameras to match the current 'facing'
        updateFacingCameras()
    }

    fun setSelectedCamera(cameraId: String) {
        if (cameraId != _selectedCameraId) {
            _selectedCameraId = cameraId
            bindUseCases()
        }
    }

    fun toggleCameraFacing() {
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
            setSelectedCamera(newCam.cameraId)
        }
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
     * For showing/hiding a shutter flash overlay when capturing a photo.
     */
    fun triggerShutterFlash() {
        showShutterFlash = true
    }

    fun onShutterFlashComplete() {
        showShutterFlash = false
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


    private fun bindUseCases() {
        // TODO: consider a job that can be interrupted?

        // These dependencies load in asynchronously, and can be destroyed & re-created at various points (e.g. rotating)
        if (previewView == null || _cameraProvider == null || lifecycleOwner == null) return;
        try {
            preview.surfaceProvider = previewView!!.surfaceProvider

            val cameraSelector = createCameraSelectorForId(_selectedCameraId)

            previewViewTouchHandlers.clear()
            _cameraProvider!!.unbindAll()

            camera = _cameraProvider!!.bindToLifecycle(
                lifecycleOwner!!,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture,
                imageAnalysis,
            )

            initializePlugins()

            _previewView = plugins.fold(previewView!!) { v, plugin ->
                plugin.onPreviewView(this, v)
            }
        } catch (e: Exception) {
            // Handle binding errors
            e.printStackTrace()

        }
    }

    fun recreateUseCases() {
        preview = createPreview()
        imageCapture = createImageCapture()
        imageAnalysis = createImageAnalysis()
        // TODO: videoCapture

        this.bindUseCases()
    }

    fun createPreview(): Preview {
        return plugins.fold(Preview.Builder()) { builder, plugin ->
            plugin.onPreview(this, builder)
        }.build()
    }

    fun createImageCapture(): ImageCapture {
        return plugins.fold(ImageCapture.Builder()) { builder, plugin ->
            plugin.onImageCapture(this, builder)
        }.build()
    }

    @OptIn(ExperimentalGetImage::class)
    fun createImageAnalysis(): ImageAnalysis {
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                    val inputImage = imageProxy.image ?: return@Analyzer

                    // Create a list to store the work objects
                    val workList = mutableListOf<CompletableDeferred<Unit>>()

                    val analysisPlugins = plugins.filter { it.onImageAnalysis != null }
                    // Dispatch work to each plugin
                    for (plugin in analysisPlugins) {
                        val work = plugin.onImageAnalysis!!(this, imageProxy, inputImage)
                        workList.add(work)
                    }

                    // Use coroutines to run the plugins in parallel and wait for all to complete
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Await all work to finish
                            workList.awaitAll()
                            Log.d("Analyzer", "All plugins completed successfully")
                        } catch (e: Exception) {
                            Log.e("Analyzer", "Error in one or more plugins", e)
                        } finally {
                            // Close the ImageProxy after all plugins are done
                            imageProxy.close()
                        }
                    }
                })
            }

        return analysis;
    }
}

class StoneCameraViewModelFactory(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val plugins: List<IPlugin>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoneCameraViewModel::class.java)) {
            return StoneCameraViewModel(context, plugins) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}