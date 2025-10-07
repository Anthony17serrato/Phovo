package com.serratocreations.phovo.data.photos.network.model

import android.content.Context
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

class AndroidNetworkFile(
    override val mediaItemDto: MediaItemDto,
    override val uri: String
) : NetworkFile, KoinComponent {
    private val context: Context by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)

    private val androidUri = uri.toUri()

    override suspend fun exists(): Boolean = withContext(ioDispatcher) {
        // Check if the URI can be opened
        return@withContext try {
            context.contentResolver.openInputStream(androidUri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun readInChunks(chunkSize: Int): Flow<ByteArray> = flow {
        context.contentResolver.openInputStream(androidUri)?.use { input ->
            val buffer = ByteArray(chunkSize)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                emit(buffer.copyOf(read)) // copy to avoid reusing buffer
            }
        }
    }.flowOn(ioDispatcher)
}