package co.stonephone.stonecamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import co.stonephone.stonecamera.utils.TranslationManager

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

    private fun isChromeOS(): Boolean {
        return packageManager.hasSystemFeature("org.chromium.arc.device_management")
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TranslationManager.loadTranslationsForLocale(applicationContext)

        if (!isChromeOS()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Hide bottom navigation bar by default
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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
