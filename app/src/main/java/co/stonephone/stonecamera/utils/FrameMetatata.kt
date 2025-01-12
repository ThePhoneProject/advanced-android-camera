package co.stonephone.stonecamera.utils

class FrameMetadata private constructor(val width: Int, val height: Int, val rotation: Float) {
    /** Builder of [FrameMetadata].  */
    class Builder {
        private var width = 0
        private var height = 0
        private var rotation = 0F

        fun setWidth(width: Int): Builder {
            this.width = width
            return this
        }

        fun setHeight(height: Int): Builder {
            this.height = height
            return this
        }

        fun setRotation(rotation: Float): Builder {
            this.rotation = rotation
            return this
        }

        fun build(): FrameMetadata {
            return FrameMetadata(width, height, rotation)
        }
    }
}