package com.serratocreations.phovo.feature.photos.util

import coil3.map.Mapper
import coil3.request.Options
import com.serratocreations.phovo.core.domain.model.DomainAssetLocation

class LocalOrRemoteAssetMapper : Mapper<DomainAssetLocation, Any> {
    override fun map(
        data: DomainAssetLocation,
        options: Options
    ): Any {
        return when (data) {
            is DomainAssetLocation.LocalAssetLocation -> data.localAssetLocation
            is DomainAssetLocation.RemoteAssetLocation -> data.remoteAssetUri
        }
    }
}