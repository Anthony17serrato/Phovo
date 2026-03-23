package com.serratocreations.phovo.core.domain

internal actual fun getThumbnailFiles(
    rootOutputDirectory: io.github.vinceglb.filekit.PlatformFile,
    dataUuid: String
): ThumbnailResources {
    // TODO
    return ThumbnailResources(
        lowResThumbnailDirectory = null,
        highResThumbnail = null
    )
}