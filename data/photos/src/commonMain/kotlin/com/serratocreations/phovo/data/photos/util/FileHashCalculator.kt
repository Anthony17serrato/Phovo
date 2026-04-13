package com.serratocreations.phovo.data.photos.util

import io.github.vinceglb.filekit.PlatformFile

interface FileHashCalculator {
    suspend fun computeSha256(file: PlatformFile): String
}