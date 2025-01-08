package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
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
import co.stonephone.stonecamera.ui.ZoomBar
import co.stonephone.stonecamera.utils.getVisibleRange
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

class FocusBasePlugin : IPlugin {
    override val id: String = "focusBase"
    override val name: String = "Focus Base"

    var focusPoint by mutableStateOf<Pair<Float, Float>?>(null)

    var viewModel: StoneCameraViewModel? = null

    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
        focusPoint = null
    }

    fun setFocusPoint(x: Float, y: Float) {
        focusPoint = Pair(x, y)
        val previewView = viewModel!!.previewView ?: return

        val factory = previewView.meteringPointFactory
        val meteringPoint = factory.createPoint(x, y)

        val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
        // TODO: I think this should keep going every few seconds to keep re-focusing on the area
        viewModel!!.camera?.cameraControl?.startFocusAndMetering(focusAction)
    }

    fun clearFocus() {
        focusPoint = null
        viewModel?.camera?.cameraControl?.cancelFocusAndMetering()
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
