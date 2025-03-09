package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

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
    fun applicationScope(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): CoroutineScope {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        return CoroutineScope(SupervisorJob() + defaultDispatcher + handler)
    }
}

expect fun getIoDispatcher(): CoroutineDispatcher