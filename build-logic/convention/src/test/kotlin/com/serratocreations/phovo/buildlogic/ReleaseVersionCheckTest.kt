package com.serratocreations.phovo.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseVersionCheckTest {

    private fun problems(version: String, branch: String, tags: List<String> = emptyList()) =
        releaseVersionProblems(PhovoVersion.parse(version), branch, tags)

    @Test
    fun `first alpha from develop is allowed`() {
        assertEquals(emptyList(), problems("1.0.0-alpha.1", "develop"))
    }

    @Test
    fun `next versions are allowed on their branches`() {
        val tags = listOf("v1.0.0-alpha.1", "v1.0.0-alpha.2", "v1.0.0")
        assertEquals(emptyList(), problems("1.0.0-alpha.3", "develop", listOf("v1.0.0-alpha.1", "v1.0.0-alpha.2")))
        assertEquals(emptyList(), problems("1.0.1", "main", tags))
        assertEquals(emptyList(), problems("1.1.0-beta.1", "release/1.1", tags))
    }

    @Test
    fun `existing tag is rejected`() {
        val result = problems("1.0.0-alpha.1", "develop", listOf("v1.0.0-alpha.1"))
        assertEquals(1, result.size)
        assertTrue(result.single().contains("already exists"))
    }

    @Test
    fun `older pre-release on the same line is rejected`() {
        val result = problems("1.0.0-alpha.2", "develop", listOf("v1.0.0-alpha.3"))
        assertTrue(result.single().contains("isn't newer than v1.0.0-alpha.3"), result.toString())
    }

    @Test
    fun `pre-release after its own stable release is rejected`() {
        val result = problems("1.1.0-beta.4", "release/1.1", listOf("v1.1.0-beta.3", "v1.1.0"))
        assertTrue(result.single().contains("v1.1.0"), result.toString())
    }

    @Test
    fun `stable hotfix ships after a newer beta or alpha line`() {
        // The README's patching flow: 1.0.1 from main while release/1.1 and develop have tagged
        // newer pre-releases. Their testers aren't offered 1.0.1, since it sorts lower.
        val tags = listOf("v1.0.0", "v1.1.0-beta.1", "v1.1.0-beta.2", "v1.2.0-alpha.1")
        assertEquals(emptyList(), problems("1.0.1", "main", tags))
    }

    @Test
    fun `beta line keeps shipping after develop tags the next alpha`() {
        val tags = listOf("v1.0.0", "v1.1.0-beta.2", "v1.2.0-alpha.1")
        assertEquals(emptyList(), problems("1.1.0-beta.3", "release/1.1", tags))
    }

    @Test
    fun `stable version older than an earlier stable release is rejected`() {
        assertTrue(problems("1.0.1", "main", listOf("v1.0.0", "v1.1.0")).single().contains("v1.1.0"))
        assertTrue(problems("1.0.0", "main", listOf("v1.0.0-alpha.1", "v1.0.1")).single().contains("v1.0.1"))
    }

    @Test
    fun `stage must match the branch`() {
        assertTrue(problems("1.0.0-alpha.1", "main").single().contains("main only publishes stable"))
        assertTrue(problems("1.0.0", "develop").single().contains("published from main"))
        assertTrue(problems("1.1.0", "release/1.1").single().contains("published from main"))
        assertTrue(problems("1.0.0-alpha.1", "feature/x").single().contains("not feature/x"))
    }

    @Test
    fun `tags outside the scheme are ignored`() {
        assertEquals(emptyList(), problems("1.0.0-alpha.1", "develop", listOf("v0.1", "vnext", "test-tag")))
    }
}
