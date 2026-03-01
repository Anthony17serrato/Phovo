package com.serratocreations.phovo

import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import kotlinx.coroutines.flow.first

class DesktopDevLogicManager(
    private val serverConfigRepository: DesktopServerConfigRepository
): DevLogicManager() {
    override suspend fun resetAppState() {
        super.resetAppState()
        val backupDir = serverConfigRepository.observeServerConfig().first()?.backupDirectory
        backupDir?.let { backupDirNotNull ->
            val dir = PlatformFile(backupDir)
            dir.delete(mustExist = false)
        }
    }
}