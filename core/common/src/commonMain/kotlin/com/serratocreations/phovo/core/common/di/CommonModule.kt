package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Named
import org.koin.core.qualifier.named
import org.koin.dsl.module

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

fun commonModule() = module {
    // TODO: Delete once all usages have been migrated to annotations
    // Coroutine scopes and dispatchers Start[DEPRECATED]
    single<CoroutineDispatcher>(named(MAIN_DISPATCHER)) { CommonModule.mainDispatcher }
    single<CoroutineDispatcher>(named(IO_DISPATCHER)) { CommonModule.ioDispatcher }
    single<CoroutineDispatcher>(named(DEFAULT_DISPATCHER)) { CommonModule.defaultDispatcher }
    single<CoroutineScope>(named(APPLICATION_SCOPE)) {
        CoroutineScope(SupervisorJob() + CommonModule.defaultDispatcher + CommonModule.handler)
    }
    // Coroutine scopes and dispatchers End[DEPRECATED]

    // Use these instead of the above
    single<CoroutineDispatcher>(named<MainDispatcher>()) { CommonModule.mainDispatcher }
    single<CoroutineDispatcher>(named<IoDispatcher>()) { CommonModule.ioDispatcher }
    single<CoroutineDispatcher>(named<DefaultDispatcher>()) { CommonModule.defaultDispatcher }
    single<CoroutineScope>(named<ApplicationScope>()) {
        CoroutineScope(SupervisorJob() + CommonModule.defaultDispatcher + CommonModule.handler)
    }
}
const val IO_DISPATCHER = "IO_DISPATCHER"
const val MAIN_DISPATCHER = "MAIN_DISPATCHER"
const val DEFAULT_DISPATCHER = "DEFAULT_DISPATCHER"
const val APPLICATION_SCOPE = "APPLICATION_SCOPE"

expect fun getIoDispatcher(): CoroutineDispatcher

internal object CommonModule {
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    val ioDispatcher: CoroutineDispatcher = getIoDispatcher()
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
}