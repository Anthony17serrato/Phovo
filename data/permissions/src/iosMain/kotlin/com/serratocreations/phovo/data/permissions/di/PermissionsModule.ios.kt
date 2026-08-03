package com.serratocreations.phovo.data.permissions.di

import com.serratocreations.phovo.data.permissions.IosPermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    single {
        IosPermissionRepository()
    } binds arrayOf(PermissionRepository::class, IosPermissionRepository::class)
}