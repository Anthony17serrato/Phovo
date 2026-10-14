package com.serratocreations.phovo.data.thumbnails

import com.serratocreations.phovo.core.model.MediaDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FfprobeVideoOutputParserTest {

    @Test
    fun unrotatedVideo_readsEverything() {
        val probe = parseFfprobeVideoOutput(output(stream = """{ "width": 320, "height": 180, "tags": {} }"""))

        assertEquals(MediaDimensions(320, 180), probe?.dimensions)
        assertEquals(1.5.seconds, probe?.duration)
        assertEquals(Instant.parse("2024-06-01T18:20:11Z"), probe?.creationTime)
    }

    @Test
    fun displayMatrixRotation_swapsDimensions() {
        val probe = parseFfprobeVideoOutput(
            output(stream = """{ "width": 1920, "height": 1080, "side_data_list": [ { "rotation": -90 } ] }""")
        )

        assertEquals(MediaDimensions(1080, 1920), probe?.dimensions)
    }

    @Test
    fun legacyRotateTag_swapsDimensions() {
        val probe = parseFfprobeVideoOutput(
            output(stream = """{ "width": 1920, "height": 1080, "tags": { "rotate": "90" } }""")
        )

        assertEquals(MediaDimensions(1080, 1920), probe?.dimensions)
    }

    @Test
    fun halfTurn_keepsDimensions() {
        val probe = parseFfprobeVideoOutput(
            output(stream = """{ "width": 1920, "height": 1080, "side_data_list": [ { "rotation": 180 } ] }""")
        )

        assertEquals(MediaDimensions(1920, 1080), probe?.dimensions)
    }

    @Test
    fun rotationNotAMultipleOf90_leavesDimensionsNull() {
        val probe = parseFfprobeVideoOutput(
            output(stream = """{ "width": 1920, "height": 1080, "side_data_list": [ { "rotation": 45 } ] }""")
        )

        assertNull(probe?.dimensions)
    }

    @Test
    fun nonPositiveSize_leavesDimensionsNull() {
        val probe = parseFfprobeVideoOutput(output(stream = """{ "width": 0, "height": 1080 }"""))

        assertNull(probe?.dimensions)
    }

    @Test
    fun noVideoStreamOrFormatTags_leavesThoseFieldsNull() {
        val probe = parseFfprobeVideoOutput("""{ "streams": [], "format": { "duration": "N/A" } }""")

        assertNull(probe?.dimensions)
        assertNull(probe?.duration)
        assertNull(probe?.creationTime)
    }

    @Test
    fun unparseableCreationTime_isNull() {
        val probe = parseFfprobeVideoOutput(
            output(stream = """{ "width": 320, "height": 180 }""", creationTime = "yesterday")
        )

        assertNull(probe?.creationTime)
        assertEquals(MediaDimensions(320, 180), probe?.dimensions)
    }

    @Test
    fun notJson_isNull() {
        assertNull(parseFfprobeVideoOutput("ffprobe: command not found"))
    }

    private fun output(stream: String, creationTime: String = "2024-06-01T18:20:11.000000Z") = """
        {
            "programs": [],
            "stream_groups": [],
            "streams": [ $stream ],
            "format": {
                "duration": "1.500000",
                "tags": { "creation_time": "$creationTime" }
            }
        }
    """.trimIndent()
}
