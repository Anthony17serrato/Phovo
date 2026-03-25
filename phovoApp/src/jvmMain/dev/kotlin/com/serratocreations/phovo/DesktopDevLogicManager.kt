package com.serratocreations.phovo

import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.flow.first

class DesktopDevLogicManager(
    private val serverConfigRepository: DesktopServerConfigRepository,
    private val getPhovoItemDao: PhovoMediaDao
): DevLogicManager() {
    override suspend fun resetAppState() {
        super.resetAppState()
        println("Reached DesktopDevLogicManager")
        val backupDir = serverConfigRepository.observeServerConfig().first()?.backupDirectory
        backupDir?.let {
            backupDir.list().forEach { file ->
                file.delete(mustExist = false)
            }
        }
        getPhovoItemDao.clearAllMediaData()
    }
}