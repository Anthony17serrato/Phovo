package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.AndroidLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.util.AndroidFileHashCalculator
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal actual fun getAndroidIosModules(): Module = module {
    single {
        HttpClient(OkHttp) {
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

    single<LocalMediaProcessor> {
        AndroidLocalMediaProcessor(
            ioDispatcher = get(IO_DISPATCHER),
            fileHashCalculator = get(),
            context = get(),
        )
    }

    factory<FileHashCalculator> {
        AndroidFileHashCalculator(ioDispatcher = get(IO_DISPATCHER))
    }
}