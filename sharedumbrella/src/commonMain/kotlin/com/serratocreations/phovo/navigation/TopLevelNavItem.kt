package com.serratocreations.phovo.navigation

import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.designsystem.model.IconAsset
import com.serratocreations.phovo.core.designsystem.model.PainterVectorIcon
import phovo.core.designsystem.generated.resources.Res as designRes
import phovo.core.designsystem.generated.resources.ic_search_rounded
import phovo.core.designsystem.generated.resources.ic_search_outlined
import com.serratocreations.phovo.feature.photos.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.feature.connections.navigation.ConnectionsHomeNavKey
import phovo.sharedumbrella.generated.resources.Res
import phovo.sharedumbrella.generated.resources.feature_search_title
import org.jetbrains.compose.resources.StringResource
import phovo.sharedumbrella.generated.resources.app_name
import phovo.feature.photos.generated.resources.feature_photos_title
import phovo.feature.photos.generated.resources.Res as photosRes
import phovo.feature.connections.generated.resources.Res as connectionsRes
import phovo.feature.connections.generated.resources.feature_connections_title
import phovo.feature.connections.generated.resources.ic_dns_rounded
import phovo.feature.connections.generated.resources.ic_dns_outlined
import phovo.sharedumbrella.generated.resources.phovo_icon

data class TopLevelNavItem(
    val selectedIcon: IconAsset,
    val unselectedIcon: IconAsset,
    val iconTextId: StringResource,
    val titleTextId: StringResource,
)

val PHOTOS = TopLevelNavItem(
    selectedIcon = PainterVectorIcon(Res.drawable.phovo_icon),
    unselectedIcon = PainterVectorIcon(Res.drawable.phovo_icon),
    iconTextId = photosRes.string.feature_photos_title,
    titleTextId = Res.string.app_name
)

val SEARCH = TopLevelNavItem(
    selectedIcon = PainterVectorIcon(designRes.drawable.ic_search_rounded),
    unselectedIcon = PainterVectorIcon(designRes.drawable.ic_search_outlined),
    iconTextId = Res.string.feature_search_title,
    titleTextId = Res.string.feature_search_title,
)

val CONNECTIONS = TopLevelNavItem(
    selectedIcon = PainterVectorIcon(connectionsRes.drawable.ic_dns_rounded),
    unselectedIcon = PainterVectorIcon(connectionsRes.drawable.ic_dns_outlined),
    iconTextId = connectionsRes.string.feature_connections_title,
    titleTextId = connectionsRes.string.feature_connections_title
)

val TOP_LEVEL_NAV_ITEMS: Map<NavKey, TopLevelNavItem> = mapOf(
    PhotosHomeNavKey to PHOTOS,
    SearchHomeNavKey to SEARCH,
    ConnectionsHomeNavKey to CONNECTIONS
)