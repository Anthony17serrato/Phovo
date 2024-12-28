package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.WasmPhovoItemDao
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao> { WasmPhovoItemDao() }
}