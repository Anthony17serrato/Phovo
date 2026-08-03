package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal actual fun getPhotosFeaturePlatformModules(): Module = module {
    viewModel {
        PhotosViewModel(
            getPhotosFeedWithThumbnailsUseCase = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    }
}