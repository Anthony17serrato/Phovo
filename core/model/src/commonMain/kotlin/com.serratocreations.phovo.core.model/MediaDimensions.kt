package com.serratocreations.phovo.core.model

/**
 * A positive width and height, in pixels.
 *
 * Media is stored with the size it is displayed at, rotation applied. Phone cameras usually encode
 * portrait photos and videos in landscape and record a rotation to apply when showing them, so the
 * size read from a file has to be turned with [rotatedBy] or [withExifOrientation] first. Every
 * platform reads that size and rotation differently, which is why it is worked out once, while the
 * media is processed, instead of by whatever later displays it.
 *
 * Only valid values are accepted. Media whose size or rotation is missing or out of range fails
 * processing, so callers check what they read before building one of these.
 */
data class MediaDimensions(
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0 && height > 0) { "Dimensions must be positive, was ${width}x$height" }
    }

    val aspectRatio: Float get() = width.toFloat() / height

    /**
     * Turned by [degrees], clockwise or counterclockwise. A quarter turn swaps the width and height.
     *
     * @param degrees a multiple of 90.
     */
    fun rotatedBy(degrees: Int): MediaDimensions {
        require(degrees % 90 == 0) { "Rotation must be a multiple of 90, was $degrees" }
        val isQuarterTurn = degrees % 180 != 0
        return if (isQuarterTurn) MediaDimensions(width = height, height = width) else this
    }

    /**
     * Turned by an EXIF orientation. Values 5 to 8 are the quarter turns, with or without a mirror,
     * and a mirror does not change the size.
     *
     * @param orientation one of [EXIF_ORIENTATIONS]. EXIF defines a missing orientation as 1.
     */
    fun withExifOrientation(orientation: Int): MediaDimensions {
        require(orientation in EXIF_ORIENTATIONS) { "EXIF orientation must be 1 to 8, was $orientation" }
        return if (orientation >= 5) rotatedBy(90) else this
    }

    companion object {
        /** The orientations EXIF defines (CIPA DC-008, tag 0x0112). 1 is the default. */
        val EXIF_ORIENTATIONS: IntRange = 1..8
    }
}
