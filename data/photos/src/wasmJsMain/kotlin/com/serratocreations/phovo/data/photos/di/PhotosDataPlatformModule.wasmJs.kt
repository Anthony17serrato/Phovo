package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.WasmPhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
internal actual class PhotosDataPlatformModule {
    @Singleton
    fun httpClient() = HttpClient(Js) {
        install(ContentNegotiation) {
            json()
        }
    }

    @Singleton
    fun phovoItemDao(): PhovoItemDao = WasmPhovoItemDao()
}