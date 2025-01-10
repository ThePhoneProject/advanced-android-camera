package co.stonephone.stonecamera.plugins

import android.content.ContentValues
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import co.stonephone.stonecamera.StoneCameraViewModel
import kotlinx.coroutines.CompletableDeferred

// Plugin interface definition
interface IPlugin {
    // The unique identifier for the plugin
    val id: String

    // The name of the plugin (for UI display purposes)
    val name: String

    // Nullable Composable render function for the plugin
    // This function takes the ViewModel and the plugin instance as parameters and renders UI
    @Composable
    fun renderViewfinder(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
    }

    @Composable
    fun renderTray(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
    }

    // Initialization function, triggered whenever the camera or previewView changes
    fun initialize(viewModel: StoneCameraViewModel)

    fun onPreview(viewModel: StoneCameraViewModel, preview: Preview.Builder): Preview.Builder {
        return preview
    }

    fun onPreviewView(viewModel: StoneCameraViewModel, previewView: PreviewView): PreviewView {
        return previewView
    }

    fun onImageCapture(
        viewModel: StoneCameraViewModel,
        imageCapture: ImageCapture.Builder
    ): ImageCapture.Builder {
        return imageCapture
    }

    fun beforeCapturePhoto(
        stoneCameraViewModel: StoneCameraViewModel,
        cv: ContentValues
    ): ContentValues {
        return cv
    }

    fun onCaptureProcessProgressed(
        stoneCameraViewModel: StoneCameraViewModel,
        progress: Int
    ) {
    }

    fun onImageSaved(
        stoneCameraViewModel: StoneCameraViewModel,
        outputFileResults: ImageCapture.OutputFileResults
    ) {
    }

    fun onCaptureStarted(stoneCameraViewModel: StoneCameraViewModel) {}

    val onImageAnalysis: ((
        viewModel: StoneCameraViewModel,
        imageProxy: ImageProxy,
        image: Image,
    ) -> CompletableDeferred<Unit>)?
        get() = null

    // Settings for the plugin
    val settings: List<PluginSetting>
}

enum class SettingLocation {
    TOP, BOTTOM, TRAY, NONE
}

// PluginSetting definition to describe settings for each plugin
sealed class PluginSetting(
    val key: String,
    val defaultValue: Any?,
    val onChange: (StoneCameraViewModel, Any?) -> Unit,
    val renderLocation: SettingLocation? = SettingLocation.NONE,
) {

    class EnumSetting(
        key: String,
        defaultValue: String,
        renderLocation: SettingLocation? = SettingLocation.NONE,
        val options: List<String>,
        val render: @Composable (value: String) -> Unit,
        onChange: (StoneCameraViewModel, String) -> Unit
    ) : PluginSetting(
        key,
        defaultValue,
        onChange as (StoneCameraViewModel, Any?) -> Unit,
        renderLocation
    )

    class ScalarSetting(
        key: String,
        defaultValue: Float,
        renderLocation: SettingLocation? = SettingLocation.NONE,
        val minValue: Float,
        val maxValue: Float,
        val stepValue: Float? = null,
        onChange: (StoneCameraViewModel, Float) -> Unit
    ) : PluginSetting(
        key,
        defaultValue,
        onChange as (StoneCameraViewModel, Any?) -> Unit,
        renderLocation
    )

    class CustomSetting(
        key: String,
        defaultValue: String,
        renderLocation: SettingLocation? = SettingLocation.NONE,
        val customRender: @Composable (StoneCameraViewModel, Any?, (Any?) -> Unit) -> Unit, // Render function with a callback for value changes
        onChange: (StoneCameraViewModel, Any?) -> Unit
    ) : PluginSetting(key, defaultValue, onChange, renderLocation)
}