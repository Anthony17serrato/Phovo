package com.serratocreations.phovo.core.logger

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

object PhovoLogger : Logger(
    config = loggerConfigInit(
        platformLogWriter(),
        platformFileLogWriter(),
        minSeverity = Severity.Info
    ),
    tag = "Phovo"
)

@Module
class LoggerCommonModule {
    @Singleton
    fun phovoLogger(): PhovoLogger = PhovoLogger
}