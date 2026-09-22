package com.serratocreations.phovo.core.workmanager.di

import com.serratocreations.phovo.core.common.di.DEFAULT_DISPATCHER
import com.serratocreations.phovo.core.workmanager.IosConstraintMonitor
import com.serratocreations.phovo.core.workmanager.IosPhovoWorkManager
import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import com.serratocreations.phovo.core.workmanager.WorkRecordStore
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual fun getAndroidIosModules(): Module = module {
    single {
        IosPhovoWorkManager(
            store = WorkRecordStore(
                defaults = NSUserDefaults.standardUserDefaults,
                logger = get()
            ),
            registry = get(),
            constraintMonitor = IosConstraintMonitor(),
            defaultDispatcher = get(DEFAULT_DISPATCHER),
            logger = get()
        )
    } binds arrayOf(PhovoWorkManager::class, IosPhovoWorkManager::class)
}
