package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.selectCameraForStepZoomLevel

class ZoomBasePlugin : IPlugin {
    override val id: String = "zoomBasePlugin"
    override val name: String = "Zoom Base"

    var currentZoom by mutableStateOf(1f)

    var viewModel: StoneCameraViewModel? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        this.viewModel = viewModel
        currentZoom = 1f
    }

    fun setZoom(zoomFactor: Float) {
        if (viewModel == null) return
        else {
            val cameras = viewModel!!.cameras
            val facingCameras =
                cameras.filter { it.lensFacing == viewModel!!.camera?.cameraInfo?.lensFacing }
            val camera = viewModel!!.camera


            // If difference in zoom is above threshold, we auto-cancel focus
//        if (kotlin.math.abs(zoomFactor - oldZoom) > ZOOM_CANCEL_THRESHOLD) {
//            cancelFocus("zoom changed by more than $ZOOM_CANCEL_THRESHOLD")
//        }

            val maxCamera = cameras.maxByOrNull { it.relativeZoom ?: 1f } ?: return
            val maxRelativeZoom = (maxCamera.relativeZoom ?: 1f) * maxCamera.maxZoom
            val minRelativeZoom =
                cameras.minByOrNull { it.relativeZoom ?: 1f }?.relativeZoom ?: 1.0f

            val newRelativeZoom = zoomFactor.coerceIn(minRelativeZoom, maxRelativeZoom)
            currentZoom = newRelativeZoom

            val (targetCamera, actualZoomRatio) = selectCameraForStepZoomLevel(
                newRelativeZoom,
                facingCameras
            )

            viewModel!!.setSelectedCamera(targetCamera.cameraId)

            camera?.cameraControl?.setZoomRatio(actualZoomRatio)
        }
    }
}
