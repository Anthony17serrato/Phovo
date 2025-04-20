package com.serratocreations.phovo.core.logger

// TODO: Implement IOS file log writer
internal actual fun platformFileLogWriter(): PlatformFileLogWriter = NoOpFileWriter()