package com.serratocreations.phovo.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseNotesStartTagTest {

    private fun start(version: String, ancestors: List<String>) =
        releaseNotesStartTag(PhovoVersion.parse(version), ancestors)

    @Test
    fun `first release has no start tag`() {
        assertNull(start("1.0.0-alpha.1", emptyList()))
    }

    @Test
    fun `pre-release starts from the previous one on its line`() {
        assertEquals("v1.0.0-alpha.2", start("1.0.0-alpha.3", listOf("v1.0.0-alpha.1", "v1.0.0-alpha.2")))
        // A stable patch merged forward into release/1.1 is an ancestor too, but on another line.
        assertEquals(
            "v1.1.0-beta.2",
            start("1.1.0-beta.3", listOf("v1.0.0", "v1.0.1", "v1.1.0-beta.1", "v1.1.0-beta.2"))
        )
    }

    @Test
    fun `first pre-release of a line starts from the latest stable release`() {
        assertEquals("v1.0.2", start("1.1.0-beta.1", listOf("v1.0.0", "v1.0.1", "v1.0.2")))
    }

    @Test
    fun `stable hotfix starts from the previous stable release, not a newer beta`() {
        // The hotfix commit's ancestors on main don't include the beta tags, but even if a merge made
        // them reachable, a stable release starts from the previous stable one.
        assertEquals("v1.0.0", start("1.0.1", listOf("v1.0.0-alpha.3", "v1.0.0", "v1.1.0-beta.2")))
    }

    @Test
    fun `stable release of a line starts from the previous stable release`() {
        assertEquals("v1.0.1", start("1.1.0", listOf("v1.0.0", "v1.0.1", "v1.1.0-beta.1", "v1.1.0-rc.1")))
    }

    @Test
    fun `later tags and tags outside the scheme are ignored`() {
        assertEquals("v1.0.0-alpha.1", start("1.0.0-alpha.2", listOf("v1.0.0-alpha.1", "v1.0.0-alpha.5", "vnext", "test")))
    }
}
