package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel

class VolumeControlsPlugin : IPlugin {
    override val id: String = "volumeControlsPlugin"
    override val name: String = "Volume Controls"

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
                    val controlModeGet = viewModel.getSetting<String>("volumeControlMode")

                    if (isZoomMode || controlModeGet == "Zoom") {
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

    override val settings: List<PluginSetting> = listOf(
        PluginSetting.EnumSetting(
            key = "volumeControlMode",
            options = listOf("Zoom", "Capture", "Smart"),
            defaultValue = "Smart",
            renderLocation = SettingLocation.TOP,
            render = { value ->
                Text(
                    value.uppercase(), color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(8.dp)
                )
            },
            onChange = { viewModel, value ->
                // NOOP
            }
        )
    )
}
