package com.serratocreations.phovo.data.server.data.mapper

import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

fun ServerConfig.asEntity() = ServerConfigEntity(
    backupDirectory = this.backupDirectory.absolutePath()
)

fun ServerConfigEntity.asExternalModel() = ServerConfig(
    backupDirectory = PlatformFile(this.backupDirectory)
)