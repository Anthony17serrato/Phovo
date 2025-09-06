package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.di.getPhotosDataModule
import com.serratocreations.phovo.data.server.di.getServerDataModule
import com.serratocreations.phovo.feature.photos.ui.PhotosViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module



fun getPhotosFeatureModule(): Module = module {
    includes(getPhotosDataModule(), getServerDataModule())

    viewModel {
        PhotosViewModel(
            get(),
            get(),
            get(IO_DISPATCHER)
        )
    }
}