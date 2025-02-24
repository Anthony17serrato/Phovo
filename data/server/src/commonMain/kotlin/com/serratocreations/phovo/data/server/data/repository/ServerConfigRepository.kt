package com.serratocreations.phovo.data.server.data.repository

import com.serratocreations.phovo.data.server.data.dao.ServerConfigDao
import com.serratocreations.phovo.data.server.data.model.ServerConfig
import org.koin.core.annotation.Singleton

@Singleton
class ServerConfigRepository(private val serverConfigDao: ServerConfigDao) {
    fun updateServerConfig(serverConfig: ServerConfig) = serverConfigDao.updateServerConfig(serverConfig)

    fun observeServerConfig() = serverConfigDao.observeServerConfig()
}