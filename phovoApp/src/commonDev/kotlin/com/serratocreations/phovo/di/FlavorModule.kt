package com.serratocreations.phovo.di

import com.serratocreations.phovo.DefaultDevLogicManager
import com.serratocreations.phovo.DevLogicManager
import com.serratocreations.phovo.viewmodel.DevViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val flavorModule = module {
    includes(getDevFlavorPlatformModule())
    viewModelOf(::DevViewModel)
    single<DevLogicManager> { DefaultDevLogicManager() }
}

expect fun getDevFlavorPlatformModule(): Module