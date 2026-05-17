package com.serratocreations.phovo.data.photos.util

import com.serratocreations.phovo.core.common.util.logTimeToComplete
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.cinterop.*
import platform.CoreCrypto.*
import kotlin.use

class IosFileHashCalculator(
    private val ioDispatcher: CoroutineDispatcher
) : FileHashCalculator {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun computeSha256(file: PlatformFile): String = logTimeToComplete(apiTag = "IosFileHashCalculator::computeSha256") {
        withContext(ioDispatcher) {
            memScoped {
                // 1. Allocate and initialize the native CommonCrypto SHA256 context
                val context = alloc<CC_SHA256_CTX>()
                CC_SHA256_Init(context.ptr)

                // 2. Stream the file content through the exact same kotlinx.io buffering setup
                file.source().buffered().use { source ->
                    val buffer = ByteArray(32 * 1024)

                    while (true) {
                        val read = source.readAtMostTo(buffer, 0, buffer.size)
                        if (read == -1) break

                        // Pin the memory of the Kotlin ByteArray to safely pass its raw address to C
                        buffer.usePinned { pinned ->
                            CC_SHA256_Update(
                                c = context.ptr,
                                data = pinned.addressOf(0),
                                len = read.toUInt()
                            )
                        }
                    }
                }

                // 3. Finalize the cryptographic hash computation
                val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
                digest.usePinned { pinnedDigest ->
                    CC_SHA256_Final(
                        md = pinnedDigest.addressOf(0).reinterpret(),
                        c = context.ptr
                    )
                }

                // 4. Converge to a lowercase hexadecimal representation matching the Android format
                digest.toHexString()
            }
        }
    }
}