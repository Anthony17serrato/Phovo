package com.serratocreations.phovo.core.serverconfig.di

import com.serratocreations.phovo.core.database.di.getDatabaseModule
import com.serratocreations.phovo.core.serverconfig.DesktopServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosWasmModules() = module {
    includes(getDatabaseModule())
    single {
        DesktopServerConfigRepository(get())
    } binds arrayOf(DesktopServerConfigRepository::class, ServerConfigRepository::class)
}