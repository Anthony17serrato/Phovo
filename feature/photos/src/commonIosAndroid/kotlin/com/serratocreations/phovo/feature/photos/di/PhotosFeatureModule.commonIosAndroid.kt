package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.feature.photos.ui.BackupStatusViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal actual fun getPhotosFeaturePlatformModules(): Module = module {
    viewModelOf(::BackupStatusViewModel)
}