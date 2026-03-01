package com.serratocreations.phovo.data.server.data.mapper

import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.data.server.data.model.ServerConfig

fun ServerConfig.asEntity() = ServerConfigEntity(
    backupDirectory = this.backupDirectory
)

fun ServerConfigEntity.asExternalModel() = ServerConfig(
    backupDirectory = this.backupDirectory
)