package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.ImageSegmenterHelper
import co.stonephone.stonecamera.utils.getBitmap
import co.stonephone.stonecamera.utils.scaleDown
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import kotlinx.coroutines.CompletableDeferred


class PortraitModePlugin : IPlugin {
    override val id: String = "portraitModePlugin"
    override val name: String = "Portrait Mode"

    private lateinit var imageSegmenterHelper: ImageSegmenterHelper
    private val deferred: CompletableDeferred<Unit> = CompletableDeferred()

    // Note the various models available here: /home/izaak/Downloads/selfie_segmenter.tflite.

    override fun initialize(viewModel: StoneCameraViewModel) {
        imageSegmenterHelper = ImageSegmenterHelper(
            context = MyApplication.getAppContext(),
            runningMode = RunningMode.IMAGE,
            currentModel = ImageSegmenterHelper.MODEL_SELFIE_SEGMENTER,
            currentDelegate = ImageSegmenterHelper.DELEGATE_CPU
        );

        imageSegmenterHelper.setupImageSegmenter();
    }

    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalGetImage::class)
    override val onImageAnalysis = { viewModel: StoneCameraViewModel,
                                     imageProxy: ImageProxy,
                                     image: Image
        ->
        println("Segmenting")
        val bitmapImage: Bitmap? = getBitmap(imageProxy)
        //512 taken from google example

        bitmapImage?.scaleDown(256F);
        val mpImage = BitmapImageBuilder(bitmapImage).build()

        val results: ImageSegmenterResult? = imageSegmenterHelper.segmentImageFile(mpImage)

        println(results)
        deferred.complete(Unit);

        deferred
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for portrait yet
}