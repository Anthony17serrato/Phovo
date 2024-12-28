package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.IosPhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao> { IosPhovoItemDao() }
}