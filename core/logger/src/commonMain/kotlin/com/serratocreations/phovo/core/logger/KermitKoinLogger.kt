package com.serratocreations.phovo.core.logger

import org.koin.core.logger.Level
import org.koin.core.logger.MESSAGE
import co.touchlab.kermit.Logger as KermitLogger
import org.koin.core.logger.Logger as KoinLogger

class KermitKoinLogger(private val logger: KermitLogger) : KoinLogger() {
    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> logger.d(msg)
            Level.INFO -> logger.i(msg)
            Level.WARNING -> logger.w(msg)
            Level.ERROR -> logger.e(msg)
            Level.NONE -> {
                // do nothing
            }
        }
    }
}