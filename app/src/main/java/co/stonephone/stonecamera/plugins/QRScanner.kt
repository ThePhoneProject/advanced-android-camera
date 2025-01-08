package co.stonephone.stonecamera.plugins

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
    var barcodePos by mutableStateOf<RectF?>(null)
    var visibleDimensions: Rect? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(viewModel: StoneCameraViewModel) {
        scanner = BarcodeScanning.getClient()
    }

    var width = 0f


    // FIX :cry:
    @Composable
    override fun renderViewfinder(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val barcodePos by remember { this::barcodePos }

        if (barcodePos !== null) {
            val width = barcodePos!!.width()
            val height = barcodePos!!.height()
            val leftPos = barcodePos!!.left
            val topPos = barcodePos!!.top

            val xOffset = (leftPos + visibleDimensions!!.left).dp
            val yOffset = (topPos + visibleDimensions!!.top).dp
            return Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(height.dp)
                    .offset(
                        x = xOffset,
                        y = yOffset
                    )
                    .border(2.dp, Color(0xFFFFCC00))
            )


        }
    }

    @Composable
    override fun renderTray(viewModel: StoneCameraViewModel, pluginInstance: IPlugin) {
        val qrValue by remember { this::value }

        return Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                            barcodePos = null
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

    @OptIn(ExperimentalGetImage::class)
    override val onImageAnalysis = { viewModel: StoneCameraViewModel,
                                     imageProxy: ImageProxy,
                                     image: Image
        ->
        val deferred = CompletableDeferred<Unit>()

        val inputImage =
            InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

        scanner?.process(inputImage)
            ?.addOnSuccessListener { barcodes ->
                if (barcodes.size > 0) {
                    val imageSize = Size(inputImage.width, inputImage.height)
                    val barcode = barcodes[0]
                    value = barcode.rawValue

                    if (barcode.boundingBox != null) {
//                        barcodePos = RectF(
//                            barcode.boundingBox!!.left.toFloat(),
//                            barcode.boundingBox!!.top.toFloat(),
//                            barcode.boundingBox!!.right.toFloat(),
//                            barcode.boundingBox!!.bottom.toFloat()
//                        )

//                        visibleDimensions =
//                            calculateImageCoverageRegion(
//                                viewModel.previewView!!,
//                                viewModel.imageCapture
//                            )
                    }
                } else {
                    value = null
                    barcodePos = null
                }
                deferred.complete(Unit)
            }
            ?.addOnFailureListener {
                // Handle failure
                Log.e("QRCode", "QR Code Detection Failed", it)
                deferred.completeExceptionally(it)
            }

        deferred
    }


    override val settings: List<PluginSetting> = emptyList() // No settings for tap-to-focus yet
}
