package com.serratocreations.phovo.core.workmanager.di

import com.serratocreations.phovo.core.workmanager.AndroidPhovoWorkManager
import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    single<PhovoWorkManager> {
        AndroidPhovoWorkManager(context = get())
    }
}
