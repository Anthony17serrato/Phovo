package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import com.serratocreations.phovo.data.photos.local.IosLocalMediaProcessor
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.photos.util.IosFileHashCalculator
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


internal actual fun getAndroidIosModules(): Module = module {
    single {
        HttpClient(Darwin) {
            expectSuccess = false
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5.minutes.inWholeMilliseconds
                connectTimeoutMillis = 15.seconds.inWholeMilliseconds
                socketTimeoutMillis = 1.minutes.inWholeMilliseconds
            }
        }
    }

    single<coil3.ImageLoader> {
        coil3.ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
            .build()
    }

    factory<FileHashCalculator> { IosFileHashCalculator(get(IO_DISPATCHER)) }

    single<LocalMediaProcessor> {
        IosLocalMediaProcessor(
            fileHashCalculator = get(),
            logger = get(),
            ioDispatcher = get(IO_DISPATCHER),
            imageLoader = get()
        )
    }
}