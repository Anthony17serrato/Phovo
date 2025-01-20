package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.data.photos.repository.PhovoItemRepository
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun photosDataModule() = module {
    includes(PhotosDataModule().module, platformModule())

    single<PhovoItemRepository> { PhovoItemRepository(get(), get(), get(named(APPLICATION_SCOPE))) }
}

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
class PhotosDataModule