package com.serratocreations.phovo.core.navigation.di

import com.serratocreations.phovo.core.navigation.NavigationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationModule = module {
    viewModel { params ->
        NavigationViewModel(
            state = params.get()
        )
    }
}