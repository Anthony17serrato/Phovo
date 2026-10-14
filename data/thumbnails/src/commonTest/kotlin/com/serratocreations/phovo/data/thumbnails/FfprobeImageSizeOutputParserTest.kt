package com.serratocreations.phovo.data.thumbnails

import com.serratocreations.phovo.core.model.MediaDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FfprobeImageSizeOutputParserTest {

    @Test
    fun tiledHeic_usesTheGridSize_notATileOrTheThumbnail() {
        // The layout of an iPhone photo: 512x512 tiles, a 320x240 thumbnail, and the grid.
        val tiles = List(48) { """{ "width": 512, "height": 512, "disposition": { "default": 0 } }""" }
        val output = """
            {
                "stream_groups": [
                    { "type": "Tile Grid", "components": [ { "width": 4032, "height": 3024 } ] }
                ],
                "streams": [
                    ${tiles.joinToString()},
                    { "width": 320, "height": 240, "disposition": { "default": 0 } }
                ]
            }
        """.trimIndent()

        assertEquals(MediaDimensions(4032, 3024), parseFfprobeImageSizeOutput(output))
    }

    @Test
    fun untiledHeicWithThumbnail_usesTheDefaultStream() {
        val output = """
            {
                "stream_groups": [],
                "streams": [
                    { "width": 320, "height": 240, "disposition": { "default": 0 } },
                    { "width": 1600, "height": 1200, "disposition": { "default": 1 } }
                ]
            }
        """.trimIndent()

        assertEquals(MediaDimensions(1600, 1200), parseFfprobeImageSizeOutput(output))
    }

    @Test
    fun jpeg_withNoDefaultStream_usesTheLargestStream() {
        val output = """
            { "stream_groups": [], "streams": [ { "width": 400, "height": 300, "disposition": { "default": 0 } } ] }
        """.trimIndent()

        assertEquals(MediaDimensions(400, 300), parseFfprobeImageSizeOutput(output))
    }

    @Test
    fun otherStreamGroupTypes_areIgnored() {
        val output = """
            {
                "stream_groups": [ { "type": "LCEVC", "components": [ { "width": 10, "height": 10 } ] } ],
                "streams": [ { "width": 400, "height": 300 } ]
            }
        """.trimIndent()

        assertEquals(MediaDimensions(400, 300), parseFfprobeImageSizeOutput(output))
    }

    @Test
    fun noUsableSize_isNull() {
        assertNull(parseFfprobeImageSizeOutput("""{ "streams": [] }"""))
        assertNull(parseFfprobeImageSizeOutput("""{ "streams": [ { "width": 0, "height": 300 } ] }"""))
        assertNull(parseFfprobeImageSizeOutput("not json"))
    }
}
