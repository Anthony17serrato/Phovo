package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.feature.connections.ui.ConnectionsViewModel
import com.serratocreations.phovo.feature.connections.ui.ServerConnectionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
    viewModelOf(::ServerConnectionsViewModel) binds arrayOf(
        ServerConnectionsViewModel::class,
        ConnectionsViewModel::class
    )
}