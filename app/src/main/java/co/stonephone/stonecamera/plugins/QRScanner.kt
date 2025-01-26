package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import co.stonephone.stonecamera.MyApplication
import co.stonephone.stonecamera.StoneCameraViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CompletableDeferred

class QRScannerPlugin : IPlugin {
    override val id: String = "qrScannerPlugin"
    override val name: String = "QR Scanner"

    var scanner: BarcodeScanner? = null
    var value by mutableStateOf<String?>(null)
    var qrScannerEnabled by mutableStateOf("ON")

    private val handler = Handler(Looper.getMainLooper())
    private var clearValueRunnable: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        value = null
        scanner = BarcodeScanning.getClient()
        qrScannerEnabled = viewModel.getSetting<String>("qrScannerEnabled").toString()
    }

    var width = 0f

    // TODO RenderViewfinder to show the box around the QR code - I spent 6hrs on it and couldn't get it to line up

    @Composable
    override fun renderTray(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val qrValue by remember { this::value }

        return Box(
            Modifier
                .fillMaxWidth()
                // TODO move to a configurable model for plugin render location
                .offset(y = -48.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    width = layoutCoordinates.size.width.toFloat()
                }
        ) {
            if (qrValue != null) {
                val maxWidth = (0.7f * width).dp
                Box(
                    Modifier
                        .background(Color(0xFFFFCC00), shape = CircleShape)
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .widthIn(0.dp, maxWidth)
                        .clickable(onClick = {
                            if (qrValue!!.startsWith("http://") || qrValue!!.startsWith("https://")) {
                                // Open in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrValue))
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(MyApplication.getAppContext(), intent, null)
                            } else if (qrValue!!.startsWith("tel:")) {
                                // Open dialer
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$qrValue"))
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(MyApplication.getAppContext(), intent, null)
                            } else if (qrValue!!.startsWith("mailto:")) {
                                // Open email client
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(qrValue))
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(MyApplication.getAppContext(), intent, null)
                            } else {
                                // Copy to clipboard
                                val clipboard =
                                    MyApplication
                                        .getAppContext()
                                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("QR Code", qrValue)
                                clipboard.setPrimaryClip(clip)
                            }

                            value = null
                        })
                ) {
                    Text(
                        text = "$qrValue",
                        color = Color.Black,
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalGetImage::class)
    override var onImageAnalysis = { viewModel: StoneCameraViewModel,
                                     imageProxy: ImageProxy,
                                     image: Image
        ->
        var deferred = CompletableDeferred<Unit>()

        if (qrScannerEnabled == "ON") {
            val inputImage =
                InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            scanner?.process(inputImage)
                ?.addOnSuccessListener { barcodes ->
                    if (barcodes.size > 0) {
                        val prevBarcode = value
                        val barcode = barcodes[0]
                        value = barcode.rawValue

                        if (value !== prevBarcode) {
                            // Trigger haptic feedback
                            val vibrator = MyApplication.getAppContext()
                                .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    100,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        }

                        // Cancel any pending clear
                        clearValueRunnable?.let { handler.removeCallbacks(it) }
                    } else {
                        if (value !== null && clearValueRunnable == null) {
                            clearValueRunnable = Runnable {
                                value = null
                                clearValueRunnable = null
                            }
                            handler.postDelayed(clearValueRunnable!!, 5000)
                        }
                    }
                    deferred.complete(Unit)
                }
                ?.addOnFailureListener {
                    // Handle failure
                    Log.e("QRCode", "QR Code Detection Failed", it)
                    deferred.completeExceptionally(it)
                }
        } else {
            deferred.complete(Unit)
        }

        deferred
    }

    override val settings: (StoneCameraViewModel) -> List<PluginSetting> = { viewModel ->
        listOf(
            PluginSetting.EnumSetting(
                key = "qrScannerEnabled",
                label = "QR Scanner",
                defaultValue = "ON",
                options = listOf("ON", "OFF"),
                render = { value, isSelected ->
                    Text(
                        text = value,
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                },
                onChange = { viewModel, value ->
                    viewModel.recreateUseCases()
                },
                renderLocation = SettingLocation.NONE
            )
        )
    }
}
