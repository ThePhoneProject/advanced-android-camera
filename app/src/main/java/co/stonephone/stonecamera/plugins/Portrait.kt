package co.stonephone.stonecamera.plugins

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.Objects
import kotlin.math.max
import kotlin.math.min

class PortraitModePlugin : IPlugin {
    override val id: String = "portraitModePlugin"
    override val name: String = "Portrait Mode"

    private val logTag: String = "PortraitMode"

    private var imagesegmenter: ImageSegmenter? = null

    init {
        setupImageSegmenter()
    }

    override fun initialize(viewModel: StoneCameraViewModel) {
        setupImageSegmenter()
    }

    override fun onImageSaved(
        stoneCameraViewModel: StoneCameraViewModel,
        outputFileResults: ImageCapture.OutputFileResults
    ) {
        val portraitModeSetting = stoneCameraViewModel.getSetting<String>("portraitMode") ?: "OFF"

        if (Objects.equals(portraitModeSetting, "OFF")) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            val contentResolver: ContentResolver = MyApplication.getAppContext().contentResolver
            val originalImageUri = outputFileResults.savedUri ?: return@launch

            val originalImage: Bitmap = getOriginalImageBitmap(contentResolver, originalImageUri)
            val rotation: Int = getOriginalImageRotation(contentResolver, originalImageUri)

            val segmentationResults: ImageSegmenterResult =
                imagesegmenter?.segment(BitmapImageBuilder(originalImage).build())
                    ?: return@launch

            // TODO try and use the confidence mask. Will give floats in range 0 => 1. Apply blur on a percent of the confidence.
            val backgroundMask: ByteBuffer =
                ByteBufferExtractor.extract(segmentationResults.categoryMask().get())

            val blurredImage: Bitmap =
                applyBlurBasedOnMask(originalImage, backgroundMask) ?: return@launch

            val rotatedBitmap = matchOriginalImageRotation(blurredImage, rotation)

            val outputStream = contentResolver.openOutputStream(originalImageUri, "w")
            outputStream?.use {
                rotatedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    100,
                    it
                )
            }
        }
    }

    private fun getOriginalImageBitmap(contentResolver: ContentResolver, imageUri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        return bitmap
    }

    private fun getOriginalImageRotation(contentResolver: ContentResolver, imageUri: Uri): Int {
        val exifInputStream = contentResolver.openInputStream(imageUri)
        val exif = ExifInterface(exifInputStream!!)
        val rotation = when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        exifInputStream.close()

        return rotation
    }

    private fun applyBlurBasedOnMask(originalImage: Bitmap, categoryMask: ByteBuffer): Bitmap? {
        categoryMask.rewind()

        val blurredBitmap = fastBlur(originalImage, categoryMask, 25)

        return blurredBitmap
    }

    private fun matchOriginalImageRotation(image: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) {
            return image;
        }
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(
            image,
            0,
            0,
            image.width,
            image.height,
            matrix,
            true
        )
    }

    // Borrowed from https://stackoverflow.com/questions/21418892/understanding-super-fast-blur-algorithm?fbclid=IwZXh0bgNhZW0CMTEAAR1w91ucNtw4nU-Z8Z9RyMYFVUHWxfgt7ivsE7foTkwR2wmdx2losQqQ0sk_aem_Zrf_8344PRxW6SFzutkE7g
    // Edited to apply the blur only on the mask
    private fun fastBlur(original: Bitmap, mask: ByteBuffer, radius: Int): Bitmap {
        if (radius < 1) {
            return original
        }

        val img = original.copy(original.config, true)

        val w = img.width
        val h = img.height
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var p1: Int
        var p2: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w.toDouble(), h.toDouble()).toInt())
        val vmax = IntArray(max(w.toDouble(), h.toDouble()).toInt())
        val pix = IntArray(w * h)

        val intMask = IntArray(w * h);

        var maskIndex = 0;
        while (mask.hasRemaining()) {
            intMask[maskIndex] = mask.get().toInt()
            maskIndex += 1
        }

        img.getPixels(pix, 0, w, 0, 0, w, h)

        val dv = IntArray(256 * div)
        i = 0
        while (i < 256 * div) {
            dv[i] = i / div
            i++
        }

        var yw = 0.also { yi = it }

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            i = -radius
            while (i <= radius) {
                p = pix[(yi + min(wm.toDouble(), max(i.toDouble(), 0.0))).toInt()]
                rsum += (p and 0xff0000) shr 16
                gsum += (p and 0x00ff00) shr 8
                bsum += p and 0x0000ff
                i++
            }
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) {
                    vmin[x] = min((x + radius + 1).toDouble(), wm.toDouble()).toInt()
                    vmax[x] = max((x - radius).toDouble(), 0.0).toInt()
                }
                p1 = pix[yw + vmin[x]]
                p2 = pix[yw + vmax[x]]

                rsum += (p1 and 0xff0000) - (p2 and 0xff0000) shr 16
                gsum += (p1 and 0x00ff00) - (p2 and 0x00ff00) shr 8
                bsum += (p1 and 0x0000ff) - (p2 and 0x0000ff)
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = (max(0.0, yp.toDouble()) + x).toInt()
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
                i++
            }
            yi = x
            y = 0
            while (y < h) {
                //TODO confirm what the mask values are (as could be multiple categories)
                if (intMask[yi] != 0) {
                    pix[yi] =
                        -0x1000000 or (dv.get(rsum) shl 16) or (dv.get(gsum) shl 8) or dv.get(bsum)
                }
                if (x == 0) {
                    vmin[y] = (min((y + radius + 1).toDouble(), hm.toDouble()) * w).toInt()
                    vmax[y] = (max((y - radius).toDouble(), 0.0) * w).toInt()
                }
                p1 = x + vmin[y]
                p2 = x + vmax[y]

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]
                yi += w
                y++
            }
            x++
        }

        img.setPixels(pix, 0, w, 0, 0, w, h)

        return img
    }

    private fun setupImageSegmenter() {
        val baseOptionsBuilder = BaseOptions.builder()
        baseOptionsBuilder.setDelegate(Delegate.GPU)
        baseOptionsBuilder.setModelAssetPath("selfie_segmenter.tflite")

        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = ImageSegmenter.ImageSegmenterOptions.builder()
                .setRunningMode(RunningMode.IMAGE)
                .setBaseOptions(baseOptions)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(true)

            val options = optionsBuilder.build()
            imagesegmenter = ImageSegmenter.createFromOptions(MyApplication.getAppContext(), options)
        } catch (e: IllegalStateException) {
            Log.e(logTag, "Image segmenter failed to load model with error: " + e.message)
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            Log.e(logTag, "Image segmenter failed to load model with error: " + e.message)
        }
    }
}