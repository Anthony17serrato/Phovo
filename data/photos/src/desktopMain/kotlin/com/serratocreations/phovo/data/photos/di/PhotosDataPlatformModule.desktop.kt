package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IoDispatcher
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.db.dao.DesktopPhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
internal actual class PhotosDataPlatformModule {
    @Singleton
    fun httpClient() = HttpClient(OkHttp)

    // Workaround for Koin Annotations bug... Can't annotate class with @Singleton
    // https://github.com/InsertKoinIO/koin-annotations/issues/249
    @Singleton
    fun phovoItemDao(
        logger: PhovoLogger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PhovoItemDao = DesktopPhovoItemDao(logger, ioDispatcher)
}