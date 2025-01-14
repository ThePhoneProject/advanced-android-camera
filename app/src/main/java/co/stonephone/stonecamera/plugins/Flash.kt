package co.stonephone.stonecamera.plugins

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.Translatable
import co.stonephone.stonecamera.utils.TranslatableString
import co.stonephone.stonecamera.utils.i18n

class FlashPlugin : IPlugin {
    override val id: String = "flashPlugin"
    override val name: String = "Flash"

    override fun initialize(viewModel: StoneCameraViewModel) {

    }

    override fun onImageCapture(
        viewModel: StoneCameraViewModel, imageCapture: ImageCapture.Builder
    ): ImageCapture.Builder {
        val flashMode = viewModel.getSetting<TranslatableString>("flash") ?: "OFF".i18n()
        val mode = flashModeStringToMode(flashMode.resolve())
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

    override val settings: (StoneCameraViewModel) -> List<PluginSetting> = { viewModel ->
        listOf(
            PluginSetting.EnumSetting(
                key = "flash",
                label = @Translatable "Flash".i18n(),
                defaultValue = @Translatable "OFF".i18n(),
                options = listOf(
                    @Translatable "OFF".i18n(),
                    @Translatable "ON".i18n(),
                    @Translatable "AUTO".i18n()
                ),
                render = { flashMode, isSelected ->
                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                "OFF".i18n() -> Icons.Default.FlashOff // Replace with your preferred icon
                                "ON".i18n() -> Icons.Default.FlashOn
                                "AUTO".i18n() -> Icons.Default.FlashAuto // You may need to add custom icons for Flash Auto
                                else -> {
                                    Icons.Default.FlashOff
                                }
                            },
                            contentDescription = when (flashMode) {
                                "OFF".i18n() -> @Translatable "Flash Off".i18n().resolve()
                                "ON".i18n() -> @Translatable "Flash On".i18n().resolve()
                                "AUTO".i18n() -> @Translatable "Flash Auto".i18n().resolve()
                                else -> {
                                    "Flash"
                                }
                            },
                            tint = if (isSelected) Color(0xFFFFCC00) else Color.White
                        )
                    }
                },
                onChange = { viewModel, value ->
                    viewModel.recreateUseCases()
                },
                renderLocation = SettingLocation.TOP
            )
        )
    }
}
