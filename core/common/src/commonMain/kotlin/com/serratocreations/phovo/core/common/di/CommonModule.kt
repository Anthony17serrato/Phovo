package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun commonModule() = module {
    // Coroutine scopes and dispatchers
    single<CoroutineDispatcher>(named(MAIN_DISPATCHER)) { CommonModule.mainDispatcher }
    single<CoroutineDispatcher>(named(IO_DISPATCHER)) { CommonModule.ioDispatcher }
    single<CoroutineDispatcher>(named(DEFAULT_DISPATCHER)) { CommonModule.defaultDispatcher }
    single<CoroutineScope>(named(APPLICATION_SCOPE)) {
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