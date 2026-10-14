package com.serratocreations.phovo.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaDimensionsTest {

    @Test
    fun constructor_nonPositiveSize_throws() {
        assertFailsWith<IllegalArgumentException> { MediaDimensions(0, 1080) }
        assertFailsWith<IllegalArgumentException> { MediaDimensions(1920, -1) }
    }

    @Test
    fun rotatedBy_quarterTurns_swapSize() {
        listOf(90, 270, -90, -270, 450).forEach { degrees ->
            assertEquals(MediaDimensions(1080, 1920), MediaDimensions(1920, 1080).rotatedBy(degrees), "degrees $degrees")
        }
    }

    @Test
    fun rotatedBy_halfAndFullTurns_keepSize() {
        listOf(0, 180, -180, 360).forEach { degrees ->
            assertEquals(MediaDimensions(1920, 1080), MediaDimensions(1920, 1080).rotatedBy(degrees), "degrees $degrees")
        }
    }

    @Test
    fun rotatedBy_notAMultipleOf90_throws() {
        assertFailsWith<IllegalArgumentException> { MediaDimensions(1920, 1080).rotatedBy(45) }
    }

    @Test
    fun withExifOrientation_quarterTurnOrientations_swapSize() {
        (5..8).forEach { orientation ->
            assertEquals(
                MediaDimensions(3000, 4000),
                MediaDimensions(4000, 3000).withExifOrientation(orientation),
                "orientation $orientation"
            )
        }
    }

    @Test
    fun withExifOrientation_otherOrientations_keepSize() {
        (1..4).forEach { orientation ->
            assertEquals(
                MediaDimensions(4000, 3000),
                MediaDimensions(4000, 3000).withExifOrientation(orientation),
                "orientation $orientation"
            )
        }
    }

    @Test
    fun withExifOrientation_outOfRange_throws() {
        listOf(0, 9, -1).forEach { orientation ->
            assertFailsWith<IllegalArgumentException>("orientation $orientation") {
                MediaDimensions(4000, 3000).withExifOrientation(orientation)
            }
        }
    }

    @Test
    fun aspectRatio_isWidthOverHeight() {
        assertEquals(0.5625f, MediaDimensions(1080, 1920).aspectRatio)
    }
}
