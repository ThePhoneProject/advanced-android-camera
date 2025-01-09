package co.stonephone.stonecamera.plugins

import androidx.camera.core.FocusMeteringAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.pxToDp
import co.stonephone.stonecamera.utils.calculateImageCoverageRegion

class FocusBasePlugin : IPlugin {
    override val id: String = "focusBase"
    override val name: String = "Focus Base"

    var focusPointDp by mutableStateOf<Pair<Dp, Dp>?>(null)

    var viewModel: StoneCameraViewModel? = null

    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
        focusPointDp = null
    }

    fun setFocusPoint(_x: Float, _y: Float) {

        val x = pxToDp(_x, MyApplication.getAppContext())
        val y = pxToDp(_y, MyApplication.getAppContext())

        val previewView = viewModel!!.previewView ?: return
        val visibleDimensions =
            calculateImageCoverageRegion(previewView, viewModel!!.imageCapture)
        val isWithinVisible =
            x >= visibleDimensions!!.left && x <= visibleDimensions!!.right && y >= visibleDimensions!!.top && y <= visibleDimensions!!.bottom

        if (!isWithinVisible) {
            clearFocus()
            return
        }

        focusPointDp = Pair((x - visibleDimensions.left).dp, (y - visibleDimensions.top).dp)

        val factory = previewView.meteringPointFactory
        val meteringPoint = factory.createPoint(_x, _y)

        val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
        // TODO: I think this should keep going every few seconds to keep re-focusing on the area
        viewModel!!.camera?.cameraControl?.startFocusAndMetering(focusAction)
    }

    fun clearFocus() {
        focusPointDp = null
        viewModel?.camera?.cameraControl?.cancelFocusAndMetering()
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
