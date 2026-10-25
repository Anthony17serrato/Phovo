package com.serratocreations.phovo.core.workmanager.di

import com.serratocreations.phovo.core.common.di.DEFAULT_DISPATCHER
import com.serratocreations.phovo.core.workmanager.AndroidPhovoWorkManager
import com.serratocreations.phovo.core.workmanager.ForegroundRuns
import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import com.serratocreations.phovo.core.workmanager.WorkerRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    single { ForegroundRuns(defaultDispatcher = get(DEFAULT_DISPATCHER), logger = get()) }

    single<PhovoWorkManager> {
        AndroidPhovoWorkManager(
            context = get(),
            runs = get(),
            // Lazy: the registry pulls in every worker registration, which have no reason to be
            // resolved just because something asked for the work manager.
            registry = lazy { get<WorkerRegistry>() },
            logger = get()
        )
    }
}
