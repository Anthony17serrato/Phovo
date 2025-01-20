package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.AndroidPhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao> { AndroidPhovoItemDao(get()) }
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}