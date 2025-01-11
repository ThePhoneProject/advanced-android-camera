package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.calculateImageCoverageRegion
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

class DebugPlugin : IPlugin {
    override val id: String = "debugPlugin"
    override val name: String = "Debug"


    var visibleRegion by mutableStateOf(null as android.graphics.Rect?)

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        visibleRegion =
            calculateImageCoverageRegion(
                viewModel.previewView!!,
                viewModel.imageCapture,
            )


    }

    @Composable
    override fun renderViewfinder(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {

        val visibleRegion = visibleRegion ?: return


        Box(
            modifier = Modifier
                .width(visibleRegion!!.width().dp)
                .height(visibleRegion!!.height().dp)
                .offset(
                    x = visibleRegion!!.left.dp,
                    y = visibleRegion!!.top.dp
                )
                .border(2.dp, Color(0xAAFFCC00))
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) { }

    }
}
