package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.feature.connections.ui.ClientConnectionsViewModel
import com.serratocreations.phovo.feature.connections.ui.ConnectionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
    viewModelOf(::ClientConnectionsViewModel) binds arrayOf(
        ClientConnectionsViewModel::class,
        ConnectionsViewModel::class
    )
}