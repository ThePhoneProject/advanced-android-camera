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

object StoneCameraAppHelpers {

    private const val TAG = "StoneCameraApp"

    /**
     * Simple photo capture logic:
     *  - create a file
     *  - call takePicture
     */
    fun capturePhoto(
        viewModel: StoneCameraViewModel,
        imageCapture: ImageCapture,
    ) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/StoneCameraApp")
        }

        contentValues = viewModel.beforeCapturePhoto(contentValues)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            MyApplication.getAppContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(MyApplication.getAppContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onCaptureProcessProgressed(progress: Int) {
                    super.onCaptureProcessProgressed(progress)

                    viewModel.onCaptureProcessProgressed(progress)
                }

                override fun onCaptureStarted() {
                    super.onCaptureStarted()

                    viewModel.onCaptureStarted()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    viewModel.onImageSaved(outputFileResults)
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
            MyApplication.getAppContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            .build()

        return videoCapture.output
            .prepareRecording(MyApplication.getAppContext(), outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(MyApplication.getAppContext())) { recordEvent ->
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
