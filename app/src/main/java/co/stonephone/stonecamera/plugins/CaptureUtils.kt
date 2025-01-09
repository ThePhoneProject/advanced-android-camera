package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

// Bad name - open to suggestions
class CaptureUtilsPlugin : IPlugin {
    override val id: String = "captureUtilsPlugin"
    override val name: String = "Capture Utils"

    var viewModel: StoneCameraViewModel? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel

        val activity = MyApplication.getAppActivity()

        // callback on volume down pressed
        activity?.let {
            it.window.callback = object : android.view.Window.Callback by it.window.callback {
                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    if (event?.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                        Log.d("CaptureUtilsPlugin", "Volume down button pressed")
                        viewModel.capturePhoto(viewModel.imageCapture)
                        return true // Consume the event
                    } else {
                        return false
                    }
                }
            }
        }

    }

    override val settings: List<PluginSetting> = emptyList()
}
