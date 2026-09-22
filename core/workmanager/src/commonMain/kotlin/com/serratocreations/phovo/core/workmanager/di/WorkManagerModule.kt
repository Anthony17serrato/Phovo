package com.serratocreations.phovo.core.workmanager.di

import com.serratocreations.phovo.core.workmanager.WorkerRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, & Android
 * this API provides modules that are specific to each individual platform
 */
internal expect fun getAndroidIosModules(): Module

fun getWorkManagerModule(): Module = module {
    // Every WorkerRegistration contributed by any module ends up here, which is what lets a
    // persisted work record name its worker by id and still be runnable after a cold start.
    single { WorkerRegistry(getAll()) }
    includes(getAndroidIosModules())
}
