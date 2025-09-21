package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.data.photos.repository.model.MediaItem

actual fun getNetworkFile(mediaItem: MediaItem): NetworkFile = DesktopNetworkFile(mediaItem)