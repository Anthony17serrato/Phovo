package com.serratocreations.phovo.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.util.getKoinInstance
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

//expect fun androidIosPlatformInitialization()
actual fun androidDesktopIosWasmPlatformInitialization() {
    val applicationScope: CoroutineScope = getKoinInstance(APPLICATION_SCOPE)
    val serverConfigRepository: ServerConfigRepository = getKoinInstance()
    val localSupportMediaRepository: LocalSupportMediaRepository = getKoinInstance()
    // TODO Should probably be moved to a periodic work manager
    applicationScope.launch {
        // Suspends until a config is available(consider withTimeout)
        val backupDirectory = serverConfigRepository.observeServerConfig()
            .mapNotNull { it?.backupDirectory }
            .distinctUntilChanged()
            .firstOrNull()
        localSupportMediaRepository.initMediaProcessing(backupDirectory)
    }
}