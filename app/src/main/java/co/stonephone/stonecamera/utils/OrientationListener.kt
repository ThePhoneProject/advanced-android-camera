package co.stonephone.stonecamera.utils

import android.content.Context
import android.view.OrientationEventListener

class OrientationListener(context: Context, private val _onOrientationChanged: (Int) -> Unit) :
    OrientationEventListener(context) {
    private var prevRotation = 0

    override fun onOrientationChanged(orientation: Int) {
        if (orientation != ORIENTATION_UNKNOWN) {
            val rotation = when {
                orientation in 330..360 || orientation in 0..30 -> 0 // Portrait
                orientation in 60..120 -> -90  // Landscape (Right)
                orientation in 150..210 -> 180 // Upside down
                orientation in 240..300 -> 90 // Landscape (Left)
                else -> prevRotation
            }
            if (prevRotation != rotation) {
                prevRotation = rotation
                _onOrientationChanged(rotation)
            }
        }
    }
}
