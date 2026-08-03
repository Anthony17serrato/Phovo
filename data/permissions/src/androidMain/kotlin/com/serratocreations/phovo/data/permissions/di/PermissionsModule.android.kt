package com.serratocreations.phovo.data.permissions.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.DEFAULT_DISPATCHER
import com.serratocreations.phovo.data.permissions.AndroidPermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    single {
        AndroidPermissionRepository(
            permissionsDataSource = get(),
            context = get(),
            appScope = get(APPLICATION_SCOPE),
            defaultDispatcher = get(DEFAULT_DISPATCHER)
        )
    } binds arrayOf(PermissionRepository::class, AndroidPermissionRepository::class)
}