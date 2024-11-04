package com.serratocreations.phovo.di

import com.serratocreations.phovo.data.db.dao.AndroidPhovoItemDao
import com.serratocreations.phovo.data.db.dao.PhovoItemDao
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PhovoItemDao> { AndroidPhovoItemDao(get()) }
}