package com.serratocreations.phovo.di

import com.serratocreations.phovo.viewmodel.DevViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val flavorModule = module {
    viewModelOf(::DevViewModel)
}