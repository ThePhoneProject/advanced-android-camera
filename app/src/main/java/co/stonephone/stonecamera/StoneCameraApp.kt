// StoneCameraApp.kt
package co.stonephone.stonecamera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import co.stonephone.stonecamera.plugins.AspectRatioPlugin
import co.stonephone.stonecamera.plugins.FlashPlugin
import co.stonephone.stonecamera.plugins.FocusBasePlugin
import co.stonephone.stonecamera.plugins.PhotoModePlugin
import co.stonephone.stonecamera.plugins.PinchToZoomPlugin
import co.stonephone.stonecamera.plugins.QRScannerPlugin
import co.stonephone.stonecamera.plugins.SettingLocation
import co.stonephone.stonecamera.plugins.ShutterFlashPlugin
import co.stonephone.stonecamera.plugins.TapToFocusPlugin
import co.stonephone.stonecamera.plugins.VolumeControlsPlugin
import co.stonephone.stonecamera.plugins.VideoModePlugin
import co.stonephone.stonecamera.plugins.ZoomBarPlugin
import co.stonephone.stonecamera.plugins.ZoomBasePlugin
import co.stonephone.stonecamera.ui.RenderPluginSetting
import co.stonephone.stonecamera.ui.StoneCameraPreview
import co.stonephone.stonecamera.utils.calculateImageCoverageRegion
import co.stonephone.stonecamera.utils.getAllCamerasInfo

// Order here is important, they are loaded and initialised in the order they are listed
// ZoomBar depends on ZoomBase, etc.
val PLUGINS = listOf(
    QRScannerPlugin(),
    ZoomBasePlugin(),
    ZoomBarPlugin(),
    FocusBasePlugin(),
    TapToFocusPlugin(),
    PinchToZoomPlugin(),
    FlashPlugin(),
    AspectRatioPlugin(),
    ShutterFlashPlugin(),
    VolumeControlsPlugin(),
    PhotoModePlugin(),
    VideoModePlugin(),
//    DebugPlugin()
)

@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun StoneCameraApp(
    cameraProvider: ProcessCameraProvider,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val stoneCameraViewModel = viewModel<StoneCameraViewModel>(
        factory = StoneCameraViewModelFactory(context, lifecycleOwner, PLUGINS)
    )

    val selectedMode = stoneCameraViewModel.selectedMode

    val plugins by remember { stoneCameraViewModel::plugins }
    val modePlugins = plugins.filter { it.modeLabel != null }
    val previewView = stoneCameraViewModel.previewView
    val imageCapture = stoneCameraViewModel.imageCapture

    val activeModePlugin = modePlugins.find { it.modeLabel == selectedMode }

    var viewfinderDimensions by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(cameraProvider, lifecycleOwner) {
        stoneCameraViewModel.onCameraProvider(cameraProvider)
        stoneCameraViewModel.onLifecycleOwner(lifecycleOwner)
    }

    LaunchedEffect(previewView, imageCapture) {
        if (previewView != null && imageCapture != null) {
            viewfinderDimensions = calculateImageCoverageRegion(
                previewView, imageCapture
            )
        }
    }

// We can load cameras once (or whenever context changes) and pass them to the ViewModel
    LaunchedEffect(Unit) {
        val allCameras = getAllCamerasInfo(context)
        stoneCameraViewModel.loadCameras(allCameras)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview behind everything
        StoneCameraPreview(
            onPreviewView = { pView -> stoneCameraViewModel.onPreviewView(pView) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stoneCameraViewModel.pluginSettings.filter { it.renderLocation == SettingLocation.TOP }
                .map { setting ->
                    RenderPluginSetting(
                        setting, stoneCameraViewModel, modifier = Modifier.padding(4.dp)
                    )
                }
        }

        viewfinderDimensions?.let {
            Box(
                modifier = Modifier
                    .width(it.width().dp)
                    .height(it.height().dp)
                    .offset(x = it.left.toFloat().dp, y = it.top.toFloat().dp)
            ) {
                plugins.map {
                    it.renderViewfinder(stoneCameraViewModel, it)
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            plugins.map {
                it.renderTray(stoneCameraViewModel, it)
            }

            // Translucent overlay for mode switch & shutter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp), contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Mode selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        modePlugins.forEach { modePlugin ->
                            val modeLabel = modePlugin.modeLabel!!
                            Text(text = modeLabel.uppercase(),
                                color = if (modeLabel == selectedMode) Color(0xFFFFCC00) else Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    stoneCameraViewModel.selectMode(modeLabel)
                                })
                        }
                    }
                    
                    activeModePlugin?.renderModeControl?.invoke()
                }
            }
        }
    }
}
