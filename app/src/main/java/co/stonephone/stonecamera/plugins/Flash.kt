package co.stonephone.stonecamera.plugins

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import co.stonephone.stonecamera.StoneCameraViewModel

class FlashPlugin : IPlugin {
    override val id: String = "flashPlugin"
    override val name: String = "Flash"

    override fun initialize(viewModel: StoneCameraViewModel) {

    }

    override fun onImageCapture(
        viewModel: StoneCameraViewModel,
        imageCapture: ImageCapture.Builder
    ): ImageCapture.Builder {
        val flashMode = viewModel.getSetting<String>("flash") ?: "OFF"
        val mode = flashModeStringToMode(flashMode)
        imageCapture.setFlashMode(mode)
        return imageCapture
    }

    private fun flashModeStringToMode(modeStr: String): Int {
        return when (modeStr.uppercase()) {
            "ON" -> ImageCapture.FLASH_MODE_ON
            "OFF" -> ImageCapture.FLASH_MODE_OFF
            "AUTO" -> ImageCapture.FLASH_MODE_AUTO
            else -> {
                Log.e("StoneCameraViewModel", "Invalid flash mode: $modeStr. Defaulting to OFF.")
                ImageCapture.FLASH_MODE_OFF
            }
        }
    }

    override val settings = { viewModel: StoneCameraViewModel ->
        listOf(
            PluginSetting.EnumSetting(
                key = "flash",
                defaultValue = "OFF",
                options = listOf("OFF", "ON", "AUTO"),
                render = { flashMode ->
                    Icon(
                        imageVector = when (flashMode) {
                            "OFF" -> Icons.Default.FlashOff // Replace with your preferred icon
                            "ON" -> Icons.Default.FlashOn
                            "AUTO" -> Icons.Default.FlashAuto // You may need to add custom icons for Flash Auto
                            else -> {
                                Icons.Default.FlashOff
                            }
                        },
                        contentDescription = when (flashMode) {
                            "OFF" -> "Flash Off"
                            "ON" -> "Flash On"
                            "AUTO" -> "Flash Auto"
                            else -> {
                                "Flash"
                            }
                        },
                        tint = Color.White // Customize as needed
                    )

                },
                onChange = { viewModel, value ->
                    viewModel.recreateUseCases()
                },
                renderLocation = SettingLocation.TOP
            )
        )
    }
}
