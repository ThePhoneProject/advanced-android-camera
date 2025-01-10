package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
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
    var isZoomMode = false

    private val handler = Handler(Looper.getMainLooper())
    private var clearValueRunnable: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
        val zoomBasePlugin = viewModel.plugins.find { it.id == "zoomBasePlugin" } as ZoomBasePlugin

        val activity = MyApplication.getAppActivity()

        activity?.let {
            it.window.callback = object : android.view.Window.Callback by it.window.callback {
                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    if (isZoomMode) {
                        clearValueRunnable?.let { handler.removeCallbacks(it) }

                        clearValueRunnable = Runnable {
                            isZoomMode = false
                            clearValueRunnable = null
                        }
                        handler.postDelayed(clearValueRunnable!!, 5000)


                        if (event?.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
                            Log.d("CaptureUtilsPlugin", "Volume up button pressed")
                            zoomBasePlugin.setZoom(zoomBasePlugin.currentZoom + 0.1f)
                            return true
                        } else if (event?.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                            Log.d("CaptureUtilsPlugin", "Volume down button pressed")
                            zoomBasePlugin.setZoom(zoomBasePlugin.currentZoom - 0.1f)
                            return true
                        } else {
                            return false
                        }
                    } else {
                        if (event?.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
                            isZoomMode = true
                            zoomBasePlugin.setZoom(zoomBasePlugin.currentZoom + 0.1f)

                            clearValueRunnable = Runnable {
                                isZoomMode = false
                                clearValueRunnable = null
                            }
                            handler.postDelayed(clearValueRunnable!!, 5000)

                            return true
                        } else if (event?.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                            Log.d("CaptureUtilsPlugin", "Volume down button pressed")
                            viewModel.capturePhoto()
                            return true
                        } else {
                            return false
                        }
                    }
                }
            }
        }

    }

    override val settings: List<PluginSetting> = emptyList()
}
