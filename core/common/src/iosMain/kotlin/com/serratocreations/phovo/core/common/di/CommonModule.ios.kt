package com.serratocreations.phovo.core.common.di

import com.serratocreations.phovo.core.common.IosPermissionManager
import com.serratocreations.phovo.core.common.PermissionManager
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

actual fun getCoreCommonPlatformModule(): Module = module {
    single<PermissionManager> { IosPermissionManager() }
}