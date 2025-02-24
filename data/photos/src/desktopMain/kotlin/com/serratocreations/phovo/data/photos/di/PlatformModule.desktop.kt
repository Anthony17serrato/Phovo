package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.db.dao.DesktopPhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

actual fun platformModule() = module {
    // TODO get() dispatcher
    single<PhovoItemDao?> { DesktopPhovoItemDao(Dispatchers.IO) }
    single<HttpClient> { HttpClient(OkHttp) }
}