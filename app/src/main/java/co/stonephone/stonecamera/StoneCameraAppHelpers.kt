// StoneCameraAppHelpers.kt (optional helper file)
package co.stonephone.stonecamera

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import co.stonephone.stonecamera.MyApplication.Companion.appContext

object StoneCameraAppHelpers {

    private const val TAG = "StoneCameraApp"

    /**
     * Simple photo capture logic:
     *  - create a file
     *  - call takePicture
     */
    fun capturePhoto(
        imageCapture: ImageCapture,
    ) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/StoneCameraApp")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            appContext.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    Log.d("StoneCameraApp", "Photo saved at: $savedUri")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("StoneCameraApp", "Photo capture failed: $exception")
                }
            }
        )
    }


    @android.annotation.SuppressLint("MissingPermission")
    fun startRecording(
        videoCapture: VideoCapture<Recorder>,
        onVideoSaved: (Uri) -> Unit
    ): Recording {
        val filename = "VID_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/StoneCameraApp")
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            appContext.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            .build()

        return videoCapture.output
            .prepareRecording(appContext, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(appContext)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "Video recording started")
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val savedUri = recordEvent.outputResults.outputUri
                            onVideoSaved(savedUri)
                            Log.d(TAG, "Video saved at: $savedUri")
                        } else {
                            Log.e(TAG, "Video recording error: ${recordEvent.error}")
                        }
                    }
                }
            }
    }
}
