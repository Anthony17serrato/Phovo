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
    fun `older version than an existing release is rejected`() {
        // A stable patch released after a newer beta line is fine; going below an existing one isn't.
        val result = problems("1.0.0-alpha.2", "develop", listOf("v1.0.0-alpha.3"))
        assertTrue(result.single().contains("isn't newer"), result.toString())
        assertTrue(result.single().contains("v1.0.0-alpha.3"))
    }

    @Test
    fun `stable patch below a newer beta line is rejected`() {
        // 1.0.1 sorts below 1.1.0-beta.1, so it can only ship while no newer line has been released.
        val result = problems("1.0.1", "main", listOf("v1.0.0", "v1.1.0-beta.1"))
        assertTrue(result.single().contains("v1.1.0-beta.1"), result.toString())
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
