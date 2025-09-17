package com.serratocreations.phovo.data.photos.network.model

import android.content.Context
import coil3.Uri
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.net.Uri as AndroidUri

class AndroidNetworkFile(
    override val uri: Uri
) : NetworkFile, KoinComponent {
    private val context: Context by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(IO_DISPATCHER)

    private val androidUri = AndroidUri.parse(uri.toString())

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