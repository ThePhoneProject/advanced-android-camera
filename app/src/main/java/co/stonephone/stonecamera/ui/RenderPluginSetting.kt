package co.stonephone.stonecamera.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.plugins.PluginSetting
import co.stonephone.stonecamera.utils.TranslatableString

@Composable
fun RenderPluginSetting(
    setting: PluginSetting, viewModel: StoneCameraViewModel, modifier: Modifier = Modifier
    // TODO modes (e.g. compact: toggle in place, row: inform parent to hide others when active, full-row: always open)
) {
    val value = viewModel.settings[setting.key] ?: setting.defaultValue
    when (setting) {
        is PluginSetting.EnumSetting -> {
            val _value = when (value) {
                is TranslatableString -> value
                else -> TranslatableString(value.toString())
            }

            Box(modifier = Modifier
                .then(modifier)
                .clickable {
                    val currentIndex = setting.options.indexOf(_value)
                    val nextIndex = (currentIndex + 1) % setting.options.size
                    viewModel.setSetting(setting.key, setting.options[nextIndex])
                }) {
                setting.render(_value, false)
            }
        }

        is PluginSetting.ScalarSetting -> {
            Slider(
                value = value as Float, onValueChange = { newValue ->
                    viewModel.setSetting(setting.key, newValue)
                }, valueRange = setting.minValue..setting.maxValue,
//                steps = setting.stepValue?.toInt()
                modifier = modifier
            )
        }

        is PluginSetting.CustomSetting -> setting.customRender(viewModel, value) { value ->
            viewModel.setSetting(setting.key, value)
        }
    }
}