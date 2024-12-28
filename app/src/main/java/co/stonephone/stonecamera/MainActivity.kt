package co.stonephone.stonecamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // List of permissions we need
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO // required if you want audio in video
    )

    // Launcher to request multiple permissions
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it == true }
            if (allGranted) {
                // All required permissions granted
                startCameraApp()
            } else {
                // Some permission was denied
                // Here you can show a dialog or disable camera features
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permissions
        if (allPermissionsGranted()) {
            // All permissions are already granted, proceed
            startCameraApp()
        } else {
            // Request the missing permissions
            requestPermissionsLauncher.launch(requiredPermissions)
        }
    }

    /**
     * Helper method to see if all required permissions are granted
     */
    private fun allPermissionsGranted() = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Once permissions are granted, set up the CameraX UI (StoneCameraApp).
     */
    private fun startCameraApp() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            setContent {
                StoneCameraApp(cameraProvider)
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
