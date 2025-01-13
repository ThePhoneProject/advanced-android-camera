package co.stonephone.stonecamera.plugins

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
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
import kotlin.math.max
import kotlin.math.min


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
        )

        imageSegmenterHelper.setupImageSegmenter()
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

        val segmentationResults: ImageSegmenterResult =
            imageSegmenterHelper.segmentImageFile(BitmapImageBuilder(bitmap).build()) ?: return

        val categoryMask: ByteBuffer =
            ByteBufferExtractor.extract(segmentationResults.categoryMask().get())

        val blurred =
            applyBlurBasedOnMask(MyApplication.getAppContext(), imageUri, categoryMask) ?: return
        blurred.saveImage(MyApplication.getAppContext())
        inputStream?.close()
    }

    // Stolen from https://stackoverflow.com/questions/21418892/understanding-super-fast-blur-algorithm?fbclid=IwZXh0bgNhZW0CMTEAAR1w91ucNtw4nU-Z8Z9RyMYFVUHWxfgt7ivsE7foTkwR2wmdx2losQqQ0sk_aem_Zrf_8344PRxW6SFzutkE7g
    // Edited to apply the blur only on the mask
    fun fastBlur(original: Bitmap, mask: ByteBuffer, radius: Int): Bitmap {
        val img = original.copy(original.config, true)
        if (radius < 1) {
            return img
        }
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

        img.getPixels(pix, 0, w, 0, 0, w, h)

        val dv = IntArray(256 * div)
        i = 0
        while (i < 256 * div) {
            dv[i] = (i / div)
            i++
        }

        yi = 0
        var yw = yi

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

                rsum += ((p1 and 0xff0000) - (p2 and 0xff0000)) shr 16
                gsum += ((p1 and 0x00ff00) - (p2 and 0x00ff00)) shr 8
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
                if (mask.get().toInt() == 0) {
                    pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
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


    fun applyBlurBasedOnMask(context: Context, imageUri: Uri, categoryMask: ByteBuffer): Bitmap? {
        // Step 1: Load the image from URI
        val capturedImage = loadBitmapFromUri(context, imageUri) ?: return null

        categoryMask.rewind()  // Reset ByteBuffer position

        val blurredBitmap = fastBlur(capturedImage, categoryMask, 25)

        return blurredBitmap
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
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        .toString() + "_test_pictures"
                )
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "img_${SystemClock.uptimeMillis()}" + ".jpeg"
            val file = File(directory, fileName)
            saveImageToStream(this, FileOutputStream(file))
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return Uri.fromFile(file)
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