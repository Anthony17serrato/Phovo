package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.network.BaseUrl
import com.serratocreations.phovo.core.model.network.Endpoint
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation

fun AssetLocation.toDomainAssetLocation(
    assetHash: String,
    endpoint: Endpoint,
    baseUrl: BaseUrl?
): DomainAssetLocation? {
    return when(this) {
        is AssetLocation.LocalAssetLocation -> {
            DomainAssetLocation.LocalAssetLocation(
                this.localAssetLocation,
                assetId = assetHash
            )
        }
        is AssetLocation.RemoteAssetLocation -> {
            DomainAssetLocation.RemoteAssetLocation(
                remoteAssetUri = this.getAssetUri(
                    assetHash = assetHash,
                    endpoint = endpoint,
                    baseUrl = baseUrl ?: run {
                        PhovoLogger.withTag("AssetLocationMapper").e {
                            "Could not map assetHash $assetHash due to missing baseUrl"
                        }
                        return null
                    }
                ),
                assetId = assetHash
            )
        }
    }
}