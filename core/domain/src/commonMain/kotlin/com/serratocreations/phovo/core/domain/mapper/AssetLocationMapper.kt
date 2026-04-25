package com.serratocreations.phovo.core.domain.mapper

import com.serratocreations.phovo.core.domain.model.DomainAssetLocation
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.photos.network.Endpoint
import com.serratocreations.phovo.data.photos.repository.model.AssetLocation

fun AssetLocation.toDomainAssetLocation(
    assetHash: String,
    endpoint: Endpoint,
    baseUrl: String?
): DomainAssetLocation? {
    return when(this) {
        is AssetLocation.LocalAssetLocation -> {
            DomainAssetLocation.LocalAssetLocation(this.localAssetLocation)
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
                )
            )
        }
    }
}