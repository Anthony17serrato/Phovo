package com.serratocreations.phovo.feature.photos.util

import coil3.map.Mapper
import coil3.request.Options
import com.serratocreations.phovo.data.photos.repository.model.LocalOrRemoteAsset

class LocalOrRemoteAssetMapper : Mapper<LocalOrRemoteAsset, Any> {
    override fun map(
        data: LocalOrRemoteAsset,
        options: Options
    ): Any {
        return when (data) {
            is LocalOrRemoteAsset.LocalAsset -> data.localAssetLocation
            is LocalOrRemoteAsset.RemoteAsset -> data.remoteAssetUri
        }
    }
}