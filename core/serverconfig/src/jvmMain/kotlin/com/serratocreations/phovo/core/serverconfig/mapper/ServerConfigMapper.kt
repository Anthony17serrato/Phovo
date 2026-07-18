package com.serratocreations.phovo.core.serverconfig.mapper

import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.core.model.ServerConfig
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

fun ServerConfig.ServerSpecificServerConfig.asEntity() = ServerConfigEntity(
    backupDirectory = this.backupDirectory.absolutePath(),
    serverName = this.serverName
)

fun ServerConfigEntity.asServerSpecificServerConfig() = ServerConfig.ServerSpecificServerConfig(
    backupDirectory = PlatformFile(this.backupDirectory),
    serverName = this.serverName
)