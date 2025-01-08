package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalContext
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.ui.ZoomBar
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


class ZoomBarPlugin : IPlugin {
    override val id: String = "zoomBarPlugin"
    override val name: String = "Zoom Bar"


    override val pluginLocation: PluginLocation?
        get() = PluginLocation.TRAY

    @Composable
    override fun render(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val zoomBasePlugin =
            remember(viewModel.plugins) { viewModel.plugins.find { it.id == "zoomBasePlugin" } as ZoomBasePlugin? }

        if (zoomBasePlugin == null) {
            return
        }

        val zoomFactor by remember { zoomBasePlugin::currentZoom }
        val facingCameras by remember { viewModel::facingCameras }

        Column(
            modifier = Modifier
                .fillMaxWidth(),

            verticalArrangement = Arrangement.SpaceBetween
        ) {
            viewModel.camera?.let { cam ->
                viewModel.cameraProvider?.let { provider ->
                    ZoomBar(
                        zoomFactor,
                        { zoomFactor -> zoomBasePlugin.setZoom(zoomFactor) },
                        cameras = facingCameras
                    )
                }
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {

    }

    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
