package com.serratocreations.phovo.core.domain.di

import com.serratocreations.phovo.data.photos.di.getPhotosDataModule
import com.serratocreations.phovo.data.server.di.getServerDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    includes(getPhotosDataModule(), getServerDataModule(), platformModule)
}

internal expect val platformModule: Module