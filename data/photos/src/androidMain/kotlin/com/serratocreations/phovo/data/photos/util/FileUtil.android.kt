package com.serratocreations.phovo.data.photos.util

import androidx.core.net.toUri
import com.serratocreations.phovo.core.model.MediaType
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher

actual suspend fun MediaType.getPlatformFile(
    uri: String,
    ioDispatcher: CoroutineDispatcher
): PlatformFile? = PlatformFile(uri.toUri())