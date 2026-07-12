package com.serratocreations.phovo.core.common.di

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val IO_DISPATCHER = named("IoDispatcher")
val MAIN_DISPATCHER = named("MainDispatcher")
val DEFAULT_DISPATCHER = named("DefaultDispatcher")
val APPLICATION_SCOPE = named("ApplicationScope")

/**
 * Useful for callback APIs which need to run on the main thread
 */
val MAIN_APPLICATION_SCOPE = named("MainApplicationScope")

fun getCoreCommonModule(): Module = module {
    single<CoroutineDispatcher>(IO_DISPATCHER) { getIoDispatcher() }
    single<CoroutineDispatcher>(MAIN_DISPATCHER) { Dispatchers.Main }
    single<CoroutineDispatcher>(DEFAULT_DISPATCHER) { Dispatchers.Default }
    single<CoroutineScope>(APPLICATION_SCOPE) {
        val logger: PhovoLogger = get()
        val defaultDispatcher: CoroutineDispatcher = get(DEFAULT_DISPATCHER)
        createApplicationScope(defaultDispatcher, logger)
    }
    single<CoroutineScope>(MAIN_APPLICATION_SCOPE) {
        val logger: PhovoLogger = get()
        val mainDispatcher: CoroutineDispatcher = get(MAIN_DISPATCHER)
        createApplicationScope(mainDispatcher, logger)
    }
}

private fun createApplicationScope(
    dispatcher: CoroutineDispatcher,
    logger: PhovoLogger
): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, exception ->
        // TODO: Add a fatalLog API to write the log to file/db in a blocking manner before
        //  crashing the process(currently it happens in a coroutine which may or may not
        //  complete before the process is killed)
        logger.withTag("CoroutineExceptionHandler").e(exception) {
            "Barnacles! @ApplicationScope CoroutineExceptionHandler caught an unhandled exception: $exception"
        }
        // re-throw the exception to crash the app
        throw exception
    }
    return CoroutineScope(SupervisorJob() + dispatcher + handler)
}

expect fun getIoDispatcher(): CoroutineDispatcher