package com.serratocreations.phovo.feature.photos.di

import com.serratocreations.phovo.feature.photos.db.dao.AndroidPhovoItemDao
import com.serratocreations.phovo.feature.photos.data.db.dao.PhovoItemDao
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao> { AndroidPhovoItemDao(get()) }
}