package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.IosPhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao> { IosPhovoItemDao() }
    single<HttpClient> {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}