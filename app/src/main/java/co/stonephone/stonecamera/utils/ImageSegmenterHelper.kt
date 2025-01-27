package co.stonephone.stonecamera.utils

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult

class ImageSegmenterHelper(
    var currentDelegate: Int = DELEGATE_CPU,
    var currentModel: Int = MODEL_DEEPLABV3,
    val context: Context,
) {

    private var imagesegmenter: ImageSegmenter? = null

    init {
        setupImageSegmenter()
    }

    // Initialize the image segmenter using current settings on the
    // thread that is using it. CPU can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the
    // segmenter
    fun setupImageSegmenter() {
        val baseOptionsBuilder = BaseOptions.builder()
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }
        }

        when(currentModel) {
            MODEL_DEEPLABV3 -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_DEEPLABV3_PATH)
            }
            MODEL_HAIR_SEGMENTER -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_HAIR_SEGMENTER_PATH)
            }
            MODEL_SELFIE_SEGMENTER -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_SELFIE_SEGMENTER_PATH)
            }
            MODEL_SELFIE_MULTICLASS -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_SELFIE_MULTICLASS_PATH)
            }
        }

        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = ImageSegmenter.ImageSegmenterOptions.builder()
                .setRunningMode(RunningMode.IMAGE)
                .setBaseOptions(baseOptions)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(false)

            val options = optionsBuilder.build()
            imagesegmenter = ImageSegmenter.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            Log.e(
                TAG,
                "Image segmenter failed to load model with error: " + e.message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            Log.e(
                TAG,
                "Image segmenter failed to load model with error: " + e.message
            )
        }
    }

    fun segmentImageFile(mpImage: MPImage): ImageSegmenterResult? {
        return imagesegmenter?.segment(mpImage)
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1

        const val MODEL_DEEPLABV3 = 0
        const val MODEL_HAIR_SEGMENTER = 1
        const val MODEL_SELFIE_SEGMENTER = 2
        const val MODEL_SELFIE_MULTICLASS = 3

        const val MODEL_DEEPLABV3_PATH = "deeplabv3.tflite"
        const val MODEL_HAIR_SEGMENTER_PATH = "hair_segmenter.tflite"
        const val MODEL_SELFIE_MULTICLASS_PATH = "selfie_multiclass.tflite"
        const val MODEL_SELFIE_SEGMENTER_PATH = "selfie_segmenter.tflite"

        private const val TAG = "ImageSegmenterHelper"

    }

}