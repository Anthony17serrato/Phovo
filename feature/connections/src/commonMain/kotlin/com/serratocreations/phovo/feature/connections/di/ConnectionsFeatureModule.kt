package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.data.server.di.getServerDataModule
import com.serratocreations.phovo.feature.connections.ui.ConnectionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module



fun getConnectionsFeatureModule(): Module = module {
    includes(getServerDataModule())

    viewModelOf(::ConnectionsViewModel)
}