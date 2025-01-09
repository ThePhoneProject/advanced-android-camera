package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.FocusReticle
import co.stonephone.stonecamera.utils.calculateImageCoverageRegion

class TapToFocusPlugin : IPlugin {
    override val id: String = "tapToFocusPlugin"
    override val name: String = "Tap to Focus"

    private var focusBasePlugin: FocusBasePlugin? = null


    @Composable
    override fun renderViewfinder(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val context = LocalContext.current
        if (focusBasePlugin == null) {
            return
        }

        val focusPointDp by remember { focusBasePlugin!!::focusPointDp }

        focusPointDp?.let { (x, y) ->
            FocusReticle(
                xDp = x,
                yDp = y,
                initialBrightness = 0f,
                onDismissFocus = {
                    focusBasePlugin?.clearFocus()
                },
                onSetBrightness = { brightness -> viewModel.setBrightness(brightness) },
                context = context
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        focusBasePlugin = viewModel.plugins.find { it.id == "focusBase" } as FocusBasePlugin?
        viewModel.registerTouchHandler { event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                focusBasePlugin?.setFocusPoint(x, y)
            }
            true
        }
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
