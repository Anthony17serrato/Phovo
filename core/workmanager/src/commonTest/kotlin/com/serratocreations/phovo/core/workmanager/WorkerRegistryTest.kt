package com.serratocreations.phovo.core.workmanager

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkerRegistryTest {

    private class NoopWorker : PhovoWorker() {
        override suspend fun doWork(): WorkResult = WorkResult.Success
    }

    @Test
    fun `creates a worker registered under an id`() {
        val registry = WorkerRegistry(listOf(WorkerRegistration("media-sync") { NoopWorker() }))
        assertNotNull(registry.create("media-sync"))
    }

    @Test
    fun `returns null for an id no longer registered`() {
        // This is what a persisted record naming a deleted worker hits after an app update.
        val registry = WorkerRegistry(listOf(WorkerRegistration("media-sync") { NoopWorker() }))
        assertNull(registry.create("removed-worker"))
    }

    @Test
    fun `builds a fresh worker per call`() {
        val registry = WorkerRegistry(listOf(WorkerRegistration("media-sync") { NoopWorker() }))
        val first = registry.create("media-sync")
        val second = registry.create("media-sync")
        assertNotNull(first)
        assertNotNull(second)
        check(first !== second) { "workers must not be shared between runs" }
    }

    @Test
    fun `registering an id twice fails at construction`() {
        // Ids are strings, so nothing stops two modules reaching for the same one. Silently letting
        // one shadow the other would make the winner depend on Koin's collection order.
        val error = assertFailsWith<IllegalArgumentException> {
            WorkerRegistry(
                listOf(
                    WorkerRegistration("media-sync") { NoopWorker() },
                    WorkerRegistration("media-sync") { NoopWorker() }
                )
            )
        }
        assertTrue("media-sync" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `distinct ids coexist`() {
        val registry = WorkerRegistry(
            listOf(
                WorkerRegistration("media-sync") { NoopWorker() },
                WorkerRegistration("thumbnails") { NoopWorker() }
            )
        )
        assertNotNull(registry.create("media-sync"))
        assertNotNull(registry.create("thumbnails"))
    }
}
