package com.serratocreations.phovo.core.logger

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import org.koin.core.module.Module
import org.koin.dsl.module

object PhovoLogger : Logger(
    config = loggerConfigInit(
        platformLogWriter(),
        platformFileLogWriter(),
        minSeverity = Severity.Info
    ),
    tag = "Phovo"
)


fun getLoggerCommonModule(): Module = module {
    single<PhovoLogger> { PhovoLogger }
}