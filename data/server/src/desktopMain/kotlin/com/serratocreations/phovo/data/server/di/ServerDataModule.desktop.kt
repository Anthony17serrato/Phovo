package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.database.di.DatabaseCommonModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// TODO Investigate warning
@Module(includes = [DatabaseCommonModule::class])
@ComponentScan("com.serratocreations.phovo.data.server")
internal actual class ServerDataPlatformModule