package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import java.security.MessageDigest
import kotlin.use

class DesktopFileHashCalculator(
    val ioDispatcher: CoroutineDispatcher
): FileHashCalculator {
    // TODO: Should handle exceptions
    override suspend fun computeSha256(file: PlatformFile): String = logTimeToComplete(apiTag = "DesktopFileHashCalculator::computeSha256") {
        withContext(ioDispatcher) {
            val digest = MessageDigest.getInstance("SHA-256")

            file.source().buffered().use { source ->
                val buffer = ByteArray(8 * 1024)

                while (true) {
                    val read = source.readAtMostTo(buffer, 0, buffer.size)
                    if (read == -1) break

                    digest.update(buffer, 0, read)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}