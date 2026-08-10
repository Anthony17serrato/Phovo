package com.serratocreations.phovo.core.common.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessingCpuBudgetTest {

    /** Machines the app is expected to run on, from a low end laptop to a workstation. */
    private val hardwareSizes = listOf(1, 2, 4, 8, 10, 16, 18, 32, 64, 128)

    /**
     * Only a single core machine has no headroom to give, every other size keeps at least one core
     * out of the budget, including the dual core case where all three modes collapse to a 1/1 split.
     */
    @Test
    fun `every mode leaves at least one core unused on machines that have one to spare`() {
        for (cores in hardwareSizes.filter { it > 1 }) {
            for (mode in ProcessingPerformanceMode.entries) {
                val budget = ProcessingCpuBudget.forMode(mode, cores)
                assertTrue(
                    budget.cpuBudget < cores,
                    "$mode on $cores cores claimed the whole machine"
                )
            }
        }
    }

    @Test
    fun `high mode reserves between two and four cores for the UI`() {
        for (cores in hardwareSizes.filter { it > 4 }) {
            val reserved = cores - ProcessingCpuBudget.forMode(
                ProcessingPerformanceMode.High,
                cores
            ).cpuBudget
            assertTrue(
                reserved in 2..4,
                "High mode on $cores cores reserved $reserved cores for the UI"
            )
        }
    }

    @Test
    fun `low end hardware always gets a usable budget`() {
        for (cores in listOf(1, 2)) {
            for (mode in ProcessingPerformanceMode.entries) {
                val budget = ProcessingCpuBudget.forMode(mode, cores)
                assertEquals(1, budget.cpuBudget, "$mode on $cores cores")
                assertEquals(1, budget.workerCount, "$mode on $cores cores")
                assertEquals(
                    1,
                    budget.maxConcurrentThumbnailProcesses,
                    "$mode on $cores cores"
                )
            }
        }
    }

    @Test
    fun `budgets increase with the performance mode`() {
        for (cores in hardwareSizes.filter { it >= 8 }) {
            val low = ProcessingCpuBudget.forMode(ProcessingPerformanceMode.Low, cores).cpuBudget
            val medium =
                ProcessingCpuBudget.forMode(ProcessingPerformanceMode.Medium, cores).cpuBudget
            val high = ProcessingCpuBudget.forMode(ProcessingPerformanceMode.High, cores).cpuBudget
            assertTrue(low < medium, "Low($low) should be below Medium($medium) on $cores cores")
            assertTrue(medium < high, "Medium($medium) should be below High($high) on $cores cores")
        }
    }

    @Test
    fun `thumbnail generation never exceeds the cpu budget`() {
        for (cores in hardwareSizes) {
            for (mode in ProcessingPerformanceMode.entries) {
                val budget = ProcessingCpuBudget.forMode(mode, cores)
                assertTrue(
                    budget.maxConcurrentThumbnailProcesses * budget.threadsPerThumbnailProcess
                        <= budget.cpuBudget,
                    "$mode on $cores cores oversubscribed its budget of ${budget.cpuBudget}"
                )
            }
        }
    }

    @Test
    fun `an absurd processor count is handled`() {
        val budget = ProcessingCpuBudget.forMode(ProcessingPerformanceMode.High, 0)
        assertEquals(1, budget.cpuBudget)
    }
}
