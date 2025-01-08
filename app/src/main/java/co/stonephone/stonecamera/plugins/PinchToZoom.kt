package co.stonephone.stonecamera.plugins

import android.view.ScaleGestureDetector
import co.stonephone.stonecamera.StoneCameraViewModel

class PinchToZoomPlugin : IPlugin {
    override val id: String = "zoomScalingPlugin"
    override val name: String = "Zoom Scaling"

    private var isScaling = false
    private var scaleGestureDetector: ScaleGestureDetector? = null

    override fun initialize(viewModel: StoneCameraViewModel) {
        val zoomBasePlugin =
            viewModel.plugins.find { it.id == "zoomBasePlugin" } as ZoomBasePlugin?

        if(zoomBasePlugin == null) {
            return
        }

        scaleGestureDetector = ScaleGestureDetector(
            viewModel.context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isScaling = true
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // Happens when camera changes during zoom
                    if(zoomBasePlugin == null) {
                        return false
                    }
                    val zoomChange = detector.scaleFactor
                    val newZoom = zoomBasePlugin.currentZoom * zoomChange
                    zoomBasePlugin.setZoom(newZoom)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isScaling = false
                }
            }
        )

        viewModel.registerTouchHandler { event ->
            scaleGestureDetector?.onTouchEvent(event)
            true
        }
    }

    override val settings: List<PluginSetting> = emptyList()
}
