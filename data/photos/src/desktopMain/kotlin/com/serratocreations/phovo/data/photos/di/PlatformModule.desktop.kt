package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.db.dao.DesktopPhovoItemDao
import com.serratocreations.phovo.data.photos.db.dao.PhovoItemDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule() = module {
    single<PhovoItemDao?> { DesktopPhovoItemDao(get(named(IO_DISPATCHER))) }
    single<HttpClient> { HttpClient(OkHttp) }
}