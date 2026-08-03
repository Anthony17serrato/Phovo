package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.feature.photos.ui.BackupStatusViewModel
import com.serratocreations.phovo.feature.photos.ui.ClientPhotosViewModel
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getPhotosFeaturePlatformModules(): Module = module {
    viewModelOf(::BackupStatusViewModel)

    viewModel {
        ClientPhotosViewModel(
            getPhotosFeedWithThumbnailsUseCase = get(),
            permissionRepository = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    } binds arrayOf(PhotosViewModel::class, ClientPhotosViewModel::class)
}