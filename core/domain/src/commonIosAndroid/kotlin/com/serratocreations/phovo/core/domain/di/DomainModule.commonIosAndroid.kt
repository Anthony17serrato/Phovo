package com.serratocreations.phovo.core.domain.di

import com.serratocreations.phovo.core.domain.GetBackupStatusUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    factory<GetBackupStatusUseCase> {
        GetBackupStatusUseCase(
            iosAndroidLocalMediaManager = get(),
            remoteMediaRepository = get()
        )
    }
}