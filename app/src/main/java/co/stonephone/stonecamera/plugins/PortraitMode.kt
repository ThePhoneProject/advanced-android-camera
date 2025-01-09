package co.stonephone.stonecamera.plugins

import co.stonephone.stonecamera.StoneCameraViewModel

class PortraitMode: IPlugin {

    override val id: String = "portraitModePlugin"
    override val name: String = "Portrait Mode"

    override fun initialize(viewModel: StoneCameraViewModel) {
        TODO("Not yet implemented")
    }

    override val settings: List<PluginSetting>
        get() = TODO("Not yet implemented")


}