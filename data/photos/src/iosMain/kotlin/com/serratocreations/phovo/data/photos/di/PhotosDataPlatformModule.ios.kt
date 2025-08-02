package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IoDispatcher
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import com.serratocreations.phovo.data.photos.db.dao.IosPhovoItemDao
import kotlinx.coroutines.CoroutineDispatcher

@Module
internal actual class PhotosDataPlatformModule {
    @Singleton
    fun httpClient() = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json()
        }
    }

    @Singleton
    fun phovoItemDao(
        logger: PhovoLogger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PhovoItemDao = IosPhovoItemDao(
        logger,
        ioDispatcher
    )
}