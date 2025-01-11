package co.stonephone.stonecamera.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.StoneCameraViewModel
import kotlinx.coroutines.launch

class SettingsTrayPlugin : IPlugin {
    override val id: String = "settingsTrayPlugin"
    override val name: String = "Settings Tray"

    var showSettingsTray by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun renderTray(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false, // Enables partially expanded states
        )
        val coroutineScope = rememberCoroutineScope()
        val plugins = viewModel.plugins

        val modifier = Modifier

        if (showSettingsTray) {
            ModalBottomSheet(
                onDismissRequest = {
                    coroutineScope.launch {
                        sheetState.hide() // Hides the bottom sheet on dismiss
                        showSettingsTray = false
                    }
                },
                sheetState = sheetState,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Black,
            ) {
                Box(
                    modifier = Modifier.padding(16.dp, top = 0.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (0.7 * LocalConfiguration.current.screenHeightDp).dp)
                    ) {
                        plugins.map { plugin ->
                            item {
                                plugin.settings(viewModel).map { setting ->
                                    val value =
                                        viewModel.getSetting(setting.key) ?: setting.defaultValue

                                    Text(
                                        text = setting.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(4.dp)
                                    )

                                    when (setting) {
                                        is PluginSetting.EnumSetting -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                setting.options.map { option ->
                                                    Box(modifier = Modifier
                                                        .then(modifier)
                                                        .clickable {
                                                            viewModel.setSetting(
                                                                setting.key, option
                                                            )
                                                        }) {
                                                        setting.render(option, option == value)
                                                    }
                                                }
                                            }
                                        }

                                        is PluginSetting.ScalarSetting -> {
                                            Slider(
                                                value = value as Float,
                                                onValueChange = { newValue ->
                                                    viewModel.setSetting(setting.key, newValue)
                                                },
                                                valueRange = setting.minValue..setting.maxValue,
                                                modifier = modifier
                                            )
                                        }

                                        is PluginSetting.CustomSetting -> setting.customRender(
                                            viewModel, value
                                        ) { value ->
                                            viewModel.setSetting(setting.key, value)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    showSettingsTray = true
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f), CircleShape
                    )
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Tray",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun initialize(viewModel: StoneCameraViewModel) {

    }
}
