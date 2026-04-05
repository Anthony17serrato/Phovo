package com.serratocreations.phovo.data.server.data.mapper

import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

fun ServerConfig.ServerSpecificServerConfig.asEntity() = ServerConfigEntity(
    backupDirectory = this.backupDirectory.absolutePath()
)

fun ServerConfigEntity.asServerSpecificServerConfig() = ServerConfig.ServerSpecificServerConfig(
    backupDirectory = PlatformFile(this.backupDirectory)
)