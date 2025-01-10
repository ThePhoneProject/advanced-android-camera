package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.ImageSegmentationUtils
import co.stonephone.stonecamera.utils.ImageSegmentationUtils.SegmentationListener
import kotlinx.coroutines.CompletableDeferred
import org.tensorflow.lite.task.vision.segmenter.Segmentation

class PortraitModePlugin: IPlugin, SegmentationListener {
    override val id: String = "portraitModePlugin"
    override val name: String = "Portrait Mode"

    private lateinit var imageSegmentationUtils: ImageSegmentationUtils
    private lateinit var bitmapBuffer: Bitmap
    private var imageAnalyzer: ImageAnalysis? = null

    override fun initialize(viewModel: StoneCameraViewModel) {
        println("Test");
        imageSegmentationUtils = ImageSegmentationUtils(
            context = MyApplication.getAppContext(),
            imageSegmentationListener = this
        )
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    override val onImageAnalysis: ((StoneCameraViewModel, ImageProxy, Image) -> CompletableDeferred<Unit>)? =
        { _: StoneCameraViewModel,
          imageProxy: ImageProxy,
          image: Image
            ->
            println("Test")
            Log.e("Test", "in here")
            val deferred = CompletableDeferred<Unit>()

            if (!::bitmapBuffer.isInitialized) {
                // The image rotation and RGB image buffer are initialized only once
                // the analyzer has started running
                bitmapBuffer = Bitmap.createBitmap(
                    image.width,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
            }

            segmentImage(imageProxy)

            deferred
        }

    //TODO Image vs imageproxy
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun segmentImage(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the image segmentation helper for processing and segmentation
        imageSegmentationUtils.segment(bitmapBuffer, imageRotation)
    }

    override val settings: List<PluginSetting> = emptyList()

    override fun onError(error: String) {
        Log.e("Segmentation", "Failed segmentation");
    }

    override fun onResults(
        results: List<Segmentation>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        Log.e("Segmentation", results.toString());
        TODO("Not yet implemented")

        //Segmentation has been complete. Can update UI to place segments on the screen.
    }
}