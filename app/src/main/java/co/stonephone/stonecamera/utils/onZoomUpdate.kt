package co.stonephone.stonecamera.utils

fun selectCameraForStepZoomLevel(
    targetRelativeZoom: Float,
    cameraInfos: List<StoneCameraInfo>
): Pair<StoneCameraInfo, Float> {

    val camerasByZoomDistance = cameraInfos.sortedByDescending { it.relativeZoom!! - targetRelativeZoom }
    val bestCamera = camerasByZoomDistance.find { it.relativeZoom!! <= targetRelativeZoom }
        ?: camerasByZoomDistance.first()

    var targetZoom = targetRelativeZoom / bestCamera.relativeZoom!!

    return Pair(bestCamera, targetZoom)
}