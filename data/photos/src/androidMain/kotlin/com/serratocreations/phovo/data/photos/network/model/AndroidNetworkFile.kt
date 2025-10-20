package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.model.network.MediaItemDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.core.net.toUri
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.source
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray

// TODO: Use PlatformFileDirectly for all platforms and delete NetworkFile interface
class AndroidNetworkFile(
    override val mediaItemDto: MediaItemDto,
    override val uri: String
) : NetworkFile, KoinComponent {
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)

    private val platformFile = PlatformFile(uri.toUri())

    override suspend fun exists(): Boolean = withContext(ioDispatcher) {
        return@withContext platformFile.exists()
    }

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> = flow {
        // Use the source to read in chunks
        platformFile.source().buffered().use { source ->
            val sink = Buffer()
            while (!source.exhausted()) {
                // Read up to chunkSize bytes, may be smaller at EOF
                val bytesRead = source.readAtMostTo(sink, chunkSize.toLong())
                if (bytesRead <= 0) break  // EOF safety
                emit(sink.readByteArray(bytesRead.toInt()))
            }
        }
    }.flowOn(ioDispatcher)
}