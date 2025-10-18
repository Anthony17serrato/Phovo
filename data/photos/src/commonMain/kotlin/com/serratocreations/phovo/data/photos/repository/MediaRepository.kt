package com.serratocreations.phovo.data.photos.repository

import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun phovoMediaFlow() : Flow<List<MediaItem>>
}