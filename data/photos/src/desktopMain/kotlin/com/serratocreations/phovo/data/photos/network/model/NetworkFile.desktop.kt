package com.serratocreations.phovo.data.photos.network.model

import com.serratocreations.phovo.core.model.network.MediaItemDto

actual fun getNetworkFile(mediaItemDto: MediaItemDto, uri: String): NetworkFile =
    DesktopNetworkFile(mediaItemDto, uri)