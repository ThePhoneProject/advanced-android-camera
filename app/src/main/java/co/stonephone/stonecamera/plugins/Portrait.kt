package co.stonephone.stonecamera.plugins

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.camera.core.ImageCapture
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.ImageSegmenterHelper
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer


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

    //TODO fix image rotation being incorrect after processing
    override fun onImageSaved(
        stoneCameraViewModel: StoneCameraViewModel,
        outputFileResults: ImageCapture.OutputFileResults
    ) {
        val contentResolver: ContentResolver = MyApplication.getAppContext().contentResolver
        val imageUri = outputFileResults.savedUri ?: return

        // Open the input stream of the original image
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

        val segmentationResults: ImageSegmenterResult = imageSegmenterHelper.segmentImageFile(BitmapImageBuilder(bitmap).build()) ?: return

        val byteBuffer: ByteBuffer = ByteBufferExtractor.extract(segmentationResults.categoryMask().get())

        val blurred = applyBlurBasedOnMask(MyApplication.getAppContext(), imageUri, byteBuffer) ?: return
        blurred.saveImage(MyApplication.getAppContext())
        inputStream?.close()
    }

    fun applyBlurBasedOnMask(context: Context, imageUri: Uri, byteBuffer: ByteBuffer): Bitmap? {
        // Step 1: Load the image from URI
        val bitmap = loadBitmapFromUri(context, imageUri) ?: return null

        // Step 2: Extract the mask from the ByteBuffer (assuming it's a binary mask with the same dimensions as the image)
        val width = bitmap.width
        val height = bitmap.height
        val maskPixels = IntArray(width * height)

        // Convert ByteBuffer to pixel values (assuming it's a grayscale mask)
        byteBuffer.rewind()  // Reset ByteBuffer position
        for (i in 0 until width * height) {
            val pixelValue = byteBuffer.get().toInt()  // Get a single byte from the buffer
            maskPixels[i] = if (pixelValue != 0) Color.WHITE else Color.BLACK
        }

        // Step 3: Apply a blur using RenderScript (or RenderEffect if on a newer Android version)
        val rs = RenderScript.create(context)
        val inputAllocation = Allocation.createFromBitmap(rs, bitmap)
        val outputAllocation = Allocation.createTyped(rs, inputAllocation.type)

        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript.setRadius(25f)  // Adjust blur radius

        blurScript.setInput(inputAllocation)
        blurScript.forEach(outputAllocation)

        val blurredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        outputAllocation.copyTo(blurredBitmap)

        // Step 4: Apply the mask: only blur pixels where the mask is non-zero
        val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)  // Draw the original bitmap on the canvas

        // Apply blur only on mask areas
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (maskPixels[y * width + x] == Color.WHITE) {
                    finalBitmap.setPixel(x, y, blurredBitmap.getPixel(x, y))
                }
            }
        }

        rs.destroy()  // Clean up RenderScript resources
        return finalBitmap
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun Bitmap.saveImage(context: Context): Uri? {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/test_pictures")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "img_${SystemClock.uptimeMillis()}")

            val uri: Uri? =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(this, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
                return uri
            }
        } else {
            val directory =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "_test_pictures")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName =  "img_${SystemClock.uptimeMillis()}"+ ".jpeg"
            val file = File(directory, fileName)
            saveImageToStream(this, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                return Uri.fromFile(file)
            }
        }
        return null
    }

    fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val settings: List<PluginSetting> = emptyList() // No settings for portrait yet

    override fun onError(error: String, errorCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onResults(resultBundle: ImageSegmenterHelper.ResultBundle) {
        TODO("Not yet implemented")
    }
}