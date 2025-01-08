package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.FocusReticle
import co.stonephone.stonecamera.utils.calculateImageCoverageRegion

class TapToFocusPlugin : IPlugin {
    override val id: String = "tapToFocusPlugin"
    override val name: String = "Tap to Focus"

    private var visibleDimensions: Rect? = null
    private var focusBasePlugin: FocusBasePlugin? = null


    @Composable
    override fun renderViewfinder(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val context = LocalContext.current
        if (focusBasePlugin == null) {
            return
        }

        val focusPoint by remember { focusBasePlugin!!::focusPoint }

        focusPoint?.let { (x, y) ->
            val visibleDimensions =
                calculateImageCoverageRegion(viewModel.previewView!!, viewModel.imageCapture)
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
            visibleDimensions =
                calculateImageCoverageRegion(previewView, viewModel.imageCapture)
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                val isWithinVisible =
                    x >= visibleDimensions!!.left && x <= visibleDimensions!!.right && y >= visibleDimensions!!.top && y <= visibleDimensions!!.bottom

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
