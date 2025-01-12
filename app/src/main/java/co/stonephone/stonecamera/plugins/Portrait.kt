package co.stonephone.stonecamera.plugins

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.camera.core.ImageCapture
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.ImageSegmenterHelper
import com.google.mediapipe.tasks.vision.core.RunningMode


class PortraitModePlugin : IPlugin, ImageSegmenterHelper.SegmenterListener {
    override val id: String = "portraitModePlugin"
    override val name: String = "Portrait Mode"

    private lateinit var imageSegmenterHelper: ImageSegmenterHelper

    // Note the various models available here: /home/izaak/Downloads/selfie_segmenter.tflite.

    override fun initialize(viewModel: StoneCameraViewModel) {
        imageSegmenterHelper = ImageSegmenterHelper(
            context = MyApplication.getAppContext(),
            runningMode = RunningMode.IMAGE,
            currentModel = ImageSegmenterHelper.MODEL_SELFIE_SEGMENTER,
            currentDelegate = ImageSegmenterHelper.DELEGATE_CPU,
            imageSegmenterListener = this
        );

        imageSegmenterHelper.setupImageSegmenter();
    }

    override fun onImageSaved(
        stoneCameraViewModel: StoneCameraViewModel,
        outputFileResults: ImageCapture.OutputFileResults
    ) {
        val contentResolver: ContentResolver = MyApplication.getAppContext().contentResolver;
        val imageUri = outputFileResults.savedUri ?: return;

        val inputStream = contentResolver.openInputStream(imageUri)

        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

        val blurred = applyBlurToBitmap(bitmap)

        val outputStream = contentResolver.openOutputStream(imageUri) ?: return
        blurred.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        inputStream?.close();
    }

    //TODO: Use something not deprecated
    fun applyBlurToBitmap(originalBitmap: Bitmap): Bitmap {
        val rs = RenderScript.create(MyApplication.getAppContext())
        val input = Allocation.createFromBitmap(rs, originalBitmap)
        val output = Allocation.createTyped(rs, input.type)

        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript.setRadius(25f) // Adjust blur radius as needed
        blurScript.setInput(input)
        blurScript.forEach(output)

        val blurredBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        output.copyTo(blurredBitmap)

        rs.destroy() // Clean up RenderScript resources
        return blurredBitmap
    }


    override val settings: List<PluginSetting> = emptyList() // No settings for portrait yet

    override fun onError(error: String, errorCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onResults(resultBundle: ImageSegmenterHelper.ResultBundle) {
        TODO("Not yet implemented")
    }
}