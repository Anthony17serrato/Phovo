package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import java.io.FileNotFoundException
import java.security.MessageDigest
import kotlin.use

class DesktopFileHashCalculator(
    val ioDispatcher: CoroutineDispatcher
): FileHashCalculator {
    // Exception handled by caller
    override suspend fun computeSha256(file: PlatformFile): String = logTimeToComplete(apiTag = "DesktopFileHashCalculator::computeSha256") {
        withContext(ioDispatcher) {
            val digest = MessageDigest.getInstance("SHA-256")

            try {
                file.source().buffered().use { source ->
                    val buffer = ByteArray(32 * 1024)

                    while (true) {
                        val read = source.readAtMostTo(buffer, 0, buffer.size)
                        if (read == -1) break

                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().toHexString()
            } catch (e: FileNotFoundException) {
                throw Exception("Failed to compute SHA-256 hash for file ${file.file.absolutePath}: ${e.message}", e)
            }
        }
    }
}