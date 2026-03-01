package com.serratocreations.phovo.core.logger

internal actual fun platformFileLogWriter(): PlatformFileLogWriter = NoOpFileWriter()