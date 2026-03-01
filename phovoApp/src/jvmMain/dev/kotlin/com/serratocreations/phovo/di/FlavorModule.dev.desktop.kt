package com.serratocreations.phovo.di

import com.serratocreations.phovo.DesktopDevLogicManager
import com.serratocreations.phovo.DevLogicManager
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.override

actual fun getDevFlavorPlatformModule(): Module = module {
    single<DevLogicManager> {
        DesktopDevLogicManager(get())
    }.override()

}