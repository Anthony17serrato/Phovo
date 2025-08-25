package com.serratocreations.phovo.core.common.di

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.qualifier.named
import org.koin.dsl.module

// TODO: Migrate from annotations back to plain Koin
@Named
annotation class MainDispatcher

@Named
annotation class DefaultDispatcher

@Named
annotation class IoDispatcher

// Coroutine scope tied to the application lifecycle; should be used sparingly for tasks that should
// not be cancelled by the callers scope cancellation(Such as DB operations).
// For more info see https://manuelvivo.dev/coroutines-cancellation-exceptions-4
// https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
@Named
annotation class ApplicationScope

@Module
@ComponentScan("com.serratocreations.phovo.core.common")
class CoreCommonModule {
    @Singleton
    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Singleton
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = getIoDispatcher()

    @Singleton
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Singleton
    @ApplicationScope
    fun applicationScope(
        logger: PhovoLogger,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        val handler = CoroutineExceptionHandler { _, exception ->
            logger.withTag("CoroutineExceptionHandler").e(exception) {
                "Barnacles! @ApplicationScope CoroutineExceptionHandler caught an unhandled exception: $exception"
            }
            // re-throw the exception to crash the app
            throw exception
        }
        return CoroutineScope(SupervisorJob() + defaultDispatcher + handler)
    }
}

val IO_DISPATCHER = named("IoDispatcher")
val MAIN_DISPATCHER = named("MainDispatcher")
val DEFAULT_DISPATCHER = named("DefaultDispatcher")
val APPLICATION_SCOPE = named("ApplicationScope")

fun getCoreCommonModule(): org.koin.core.module.Module = module {
    single<CoroutineDispatcher>(IO_DISPATCHER) { getIoDispatcher() }
    single<CoroutineDispatcher>(MAIN_DISPATCHER) { Dispatchers.Main }
    single<CoroutineDispatcher>(DEFAULT_DISPATCHER) { Dispatchers.Default }
    single<CoroutineScope>(APPLICATION_SCOPE) {
        val logger: PhovoLogger = get()
        val defaultDispatcher: CoroutineDispatcher = get(DEFAULT_DISPATCHER)
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
        CoroutineScope(SupervisorJob() + defaultDispatcher + handler)
    }
}

expect fun getIoDispatcher(): CoroutineDispatcher