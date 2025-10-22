package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.model.MediaType
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher

// TODO add option to get Icloud/Gphotos cloud images
expect suspend fun MediaType.getPlatformFile(
    uri: String,
    ioDispatcher: CoroutineDispatcher
) : PlatformFile?