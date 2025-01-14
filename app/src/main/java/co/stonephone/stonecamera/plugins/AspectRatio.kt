package co.stonephone.stonecamera.plugins

import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import co.stonephone.stonecamera.utils.Translatable
import co.stonephone.stonecamera.utils.TranslatableString
import co.stonephone.stonecamera.utils.getLargestMatchingSize
import co.stonephone.stonecamera.utils.getLargestSensorSize
import co.stonephone.stonecamera.utils.i18n

class AspectRatioPlugin : IPlugin {
    override val id: String = "aspectRatioPlugin"
    override val name: String = "Aspect Ratio"

    override fun initialize(viewModel: StoneCameraViewModel) {
        val previewView = viewModel.previewView
        val aspectRatio = viewModel.getSetting<TranslatableString>("aspectRatio")
        if (aspectRatio?.resolve() === "FULL") {
            previewView!!.scaleType = PreviewView.ScaleType.FILL_CENTER
        } else {
            previewView!!.scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    override fun onImageCapture(
        viewModel: StoneCameraViewModel,
        imageCapture: ImageCapture.Builder
    ): ImageCapture.Builder {
        // TODO "FULL" aspect ratio isn't cropping on image capture
        val ratioStr = viewModel.getSetting<TranslatableString>("aspectRatio") ?: "16:9".i18n()
        val context = MyApplication.getAppContext()

        val ratioOrNull = parseRatioOrNull(ratioStr.resolve())

        val targetSize = if (ratioOrNull == null) {
            getLargestSensorSize(viewModel.camera!!, context)
        } else {
            getLargestMatchingSize(viewModel.camera!!, context, ratioOrNull)
        }

        val resolutionSelector = buildResolutionSelector(targetSize, ratioOrNull)

        return imageCapture
            .setResolutionSelector(resolutionSelector)
    }

    override fun onPreview(
        viewModel: StoneCameraViewModel,
        preview: Preview.Builder
    ): Preview.Builder {
        val ratioStr = viewModel.getSetting<TranslatableString>("aspectRatio") ?: "16:9".i18n()

        val context = MyApplication.getAppContext()
        val ratioOrNull = parseRatioOrNull(ratioStr.resolve())

        // 1) Figure out the best "preferred size" for this ratio
        val targetSize = if (ratioOrNull == null) {
            getLargestSensorSize(viewModel.camera!!, context)
        } else {
            getLargestMatchingSize(viewModel.camera!!, context, ratioOrNull)
        }

        // 2) Build a ResolutionSelector
        val resolutionSelector = buildResolutionSelector(targetSize, ratioOrNull)

        // 3) Apply to the Preview builder
        return preview
            .setResolutionSelector(resolutionSelector)
    }

    /**
     * Build a ResolutionSelector that "prefers" [targetSize] if not null,
     * otherwise tries to fallback to an aspect ratio if [ratio] is non-null,
     * or does default if both are null.
     */
    private fun buildResolutionSelector(
        targetSize: Size?,     // e.g. 3000×3000 for 1:1
        ratio: Float?          // e.g. 1.0f for 1:1, 1.333...f for 4:3, etc.
    ): ResolutionSelector {
        val aspectRatioConst = when {
            ratio == null -> null // “FULL” or unknown
            kotlin.math.abs(ratio - (4f / 3f)) < 0.01f -> AspectRatio.RATIO_4_3
            kotlin.math.abs(ratio - (16f / 9f)) < 0.01f -> AspectRatio.RATIO_16_9
            else -> null // e.g. 1:1 or any custom ratio
        }

        // If no targetSize is found, rely entirely on aspectRatioConst or a fallback
        if (targetSize == null) {
            // No recognized ratio => default to RATIO_4_3 fallback
            return if (aspectRatioConst == null) {
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_4_3,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            } else {
                // Use the recognized built-in ratio
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            aspectRatioConst,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            }
        }

        // We do have a targetSize (the largest size matching the custom ratio, or “FULL” sensor size).
        val resolutionStrategy = ResolutionStrategy(
            targetSize,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )

        // If ratio maps to 4:3 or 16:9, we can also supply an AspectRatioStrategy fallback
        return if (aspectRatioConst != null) {
            ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        aspectRatioConst,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()
        } else {
            // ratio is something else (e.g. 1:1), so no built-in aspect ratio
            // rely on the resolutionStrategy alone
            ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .build()
        }
    }

    override fun onPreviewView(
        viewModel: StoneCameraViewModel,
        previewView: PreviewView
    ): PreviewView {
        val aspectRatio = viewModel.getSetting<TranslatableString>("aspectRatio")
        if (aspectRatio?.resolve() == "FULL") {
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        } else {
            previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        }
        return previewView
    }


    private fun parseRatioOrNull(ratioStr: String): Float? {
        val nums = ratioStr.split(":").map { it.toFloatOrNull() }
        if (nums.any { it == null }) return null
        return nums[0]!! / nums[1]!!
    }

    override val settings = { viewModel: StoneCameraViewModel ->
        listOf(
            PluginSetting.EnumSetting(
                key = "aspectRatio",
                defaultValue = @Translatable "16:9".i18n(),
                options = listOf(@Translatable "16:9".i18n(), @Translatable "4:3".i18n(), @Translatable "FULL".i18n()),
                render = { value, isSelected ->
                    Text(
                        text = value.resolve(),
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                },
                onChange = { viewModel, _ ->
                    val previewView = viewModel.previewView
                    if (viewModel.getSetting<TranslatableString>("aspectRatio")?.resolve() === "FULL") {
                        previewView!!.scaleType = PreviewView.ScaleType.FILL_CENTER
                    } else {
                        previewView!!.scaleType = PreviewView.ScaleType.FIT_CENTER
                    }
                    viewModel.recreateUseCases()
                },
                renderLocation = SettingLocation.TOP,
                label = @Translatable "Aspect Ratio".i18n()
            )
        )
    }
}
