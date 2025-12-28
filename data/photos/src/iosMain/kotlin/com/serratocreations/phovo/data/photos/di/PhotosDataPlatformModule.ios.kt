package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import com.serratocreations.phovo.data.photos.local.IosLocalMediaProcessor
import org.koin.core.module.Module
import org.koin.dsl.module


internal actual fun getAndroidIosModules(): Module = module {
    single {
        HttpClient(Darwin) {
            expectSuccess = false
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single<LocalMediaProcessor> {
        IosLocalMediaProcessor(
            logger = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    }
}