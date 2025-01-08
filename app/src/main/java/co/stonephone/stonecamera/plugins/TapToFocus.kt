package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.FocusReticle
import co.stonephone.stonecamera.utils.getVisibleRange

class TapToFocusPlugin : IPlugin {
    override val id: String = "tapToFocusPlugin"
    override val name: String = "Tap to Focus"

    private var visibleDimensions: List<Float>? = null
    private var focusBasePlugin: FocusBasePlugin? = null

    override val pluginLocation: PluginLocation?
        get() = PluginLocation.VIEWFINDER

    @Composable
    override fun render(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val context = LocalContext.current
        if (focusBasePlugin == null) {
            return
        }

        val focusPoint by remember { focusBasePlugin!!::focusPoint }

        focusPoint?.let { (x, y) ->
            val visibleDimensions = getVisibleRange(viewModel.previewView!!, viewModel.imageCapture)
            visibleDimensions?.let { dimensions ->
                FocusReticle(
                    x = x,
                    y = y,
                    initialBrightness = 0f,
                    onDismissFocus = {
                        focusBasePlugin?.clearFocus()
                    },
                    onSetBrightness = { brightness -> viewModel.setBrightness(brightness) },
                    visibleDimensions = dimensions,
                    context = context
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        focusBasePlugin = viewModel.plugins.find { it.id == "focusBase" } as FocusBasePlugin?
        val previewView = viewModel.previewView ?: return
        viewModel.registerTouchHandler { event ->
            visibleDimensions = getVisibleRange(previewView, viewModel.imageCapture)
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                val (topOfVisible, bottomOfVisible, leftOfVisible, rightOfVisible) = visibleDimensions
                    ?: return@registerTouchHandler false
                val isWithinVisible =
                    x >= leftOfVisible && x <= rightOfVisible && y >= topOfVisible && y <= bottomOfVisible

                if (!isWithinVisible) {
                    focusBasePlugin?.clearFocus()
                    return@registerTouchHandler false
                }

                focusBasePlugin?.setFocusPoint(x, y)
            }
            true
        }
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
