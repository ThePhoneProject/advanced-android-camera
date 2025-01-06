package co.stonephone.stonecamera.ui

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.stonephone.stonecamera.utils.StoneCameraInfo
import kotlin.math.roundToInt

@Composable
fun ZoomBar(
    camera: Camera,
    cameraProvider: ProcessCameraProvider,
    relativeZoom: Float,
    setZoom: (zoomFactor: Float) -> Unit,
    context: Context,
    cameras: List<StoneCameraInfo>,
    modifier: Modifier = Modifier
) {
    val currentRelativeZoom by rememberUpdatedState(newValue = relativeZoom)

    // We don’t store local camera or zoom factor states here.
    // The parent (ViewModel) has the source of truth.
    // Just compute ephemeral data from those inputs.

    // Calculate the maximum possible relative zoom
    val maxCamera = cameras.maxByOrNull { it.relativeZoom ?: 1f }
    val maxRelativeZoom = (maxCamera?.relativeZoom ?: 1f) * (maxCamera?.maxZoom ?: 1f)
    val minRelativeZoom = cameras.minByOrNull { it.relativeZoom!! }?.relativeZoom

    val zoomToAdd = (maxRelativeZoom / 2).roundToInt().toFloat()

    // Build a list of zoom stops
    val zoomStops = remember(cameras, maxRelativeZoom) {
        var baseStops = cameras
            .map { it.relativeZoom ?: 1f }
            .sorted()

        // Ensure the maximum is included
        if (zoomToAdd > baseStops.max()) {
            baseStops = (baseStops + zoomToAdd)
        }
        baseStops.distinct().sorted()
    }

    // For the highlight logic, we’ll round the current parent-provided zoom
    val roundedCurrentZoom = (relativeZoom * 10f).roundToInt() / 10f

    // Provide gesture detection (pinch & drag)
    var isDragging by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }

    // Figure out a rough “stop gap” for the drag gesture
    val (lowerStop, upperStop) = remember(relativeZoom, zoomStops) {
        val lower = zoomStops.sortedDescending().find { it <= relativeZoom } ?: zoomStops.first()
        val upper = zoomStops.filter { it != lower }.sorted().find { it > relativeZoom }
            ?: zoomStops.last()
        lower to upper
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Pinch gesture
            .pointerInput(
                relativeZoom, setZoom
            ) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val newZoom = (relativeZoom * zoomChange)
                    setZoom(newZoom)
                }
            }
            // Drag gesture
            .pointerInput(cameras) {
                val sortedCameras = cameras.sortedBy { it.relativeZoom }

                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        var draggedZoom = currentRelativeZoom
                        change.consume()
                        val lowClosest =
                            sortedCameras
                                .reversed()
                                .find { it.relativeZoom!! <= draggedZoom }
                                ?: sortedCameras.first()
                        val highClosest =
                            sortedCameras.find { it.relativeZoom!! >= draggedZoom }
                                ?: sortedCameras.last()
                        var stopGap = (highClosest.relativeZoom!! - lowClosest.relativeZoom!!)
                        if (stopGap == 0f) {
                            stopGap = 5f
                        }
                        draggedZoom =
                            (draggedZoom + (dragAmount.x / (500f / stopGap))).coerceIn(
                                minRelativeZoom,
                                maxRelativeZoom
                            )
                        Log.d(
                            "Zoom",
                            "Relative Zoom: $relativeZoom, Dragged Zoom: $draggedZoom"
                        )
                        setZoom(draggedZoom)
                    }
                )
            }
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Render preset “zoom-stop” bubbles
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(2.dp, 2.dp)
        ) {
            zoomStops.forEach { stop ->
                val isActiveStop = stop === lowerStop
                val roundedStop = (stop * 10f).roundToInt() / 10f
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isActiveStop) Color.Black.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        )
                        .padding(8.dp)
                        .clickable {
                            setZoom(stop)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isActiveStop) "${formatZoom(relativeZoom)}×" else "${
                            formatZoom(
                                roundedStop
                            )
                        }",
                        color = if (isActiveStop) Color(0xFFFFCC00) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


fun formatZoom(value: Float): String {
    return when {
        value % 1 == 0f -> value.toInt().toString() // Whole number
        value < 1 -> ".${(value * 10).toInt()}"    // 0.x case
        else -> "%.1f".format(value)              // Decimal to 1dp
    }
}