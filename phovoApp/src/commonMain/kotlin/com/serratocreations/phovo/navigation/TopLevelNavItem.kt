package com.serratocreations.phovo.navigation

import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.designsystem.model.IconAsset
import com.serratocreations.phovo.core.designsystem.model.ImageVectorIcon
import com.serratocreations.phovo.core.designsystem.model.PainterVectorIcon
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.feature.photos.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.feature.connections.ui.ConnectionsRouteComponent
import phovo.phovoapp.generated.resources.Res
import phovo.phovoapp.generated.resources.feature_search_title
import org.jetbrains.compose.resources.StringResource
import phovo.phovoapp.generated.resources.app_name
import phovo.feature.photos.generated.resources.feature_photos_title
import phovo.feature.photos.generated.resources.Res as photosRes
import phovo.feature.connections.generated.resources.Res as connectionsRes
import phovo.feature.connections.generated.resources.feature_connections_title
import phovo.phovoapp.generated.resources.phovo_icon

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
    selectedIcon = ImageVectorIcon(PhovoIcons.Search),
    unselectedIcon = ImageVectorIcon(PhovoIcons.SearchBorder),
    iconTextId = Res.string.feature_search_title,
    titleTextId = Res.string.feature_search_title,
)

val CONNECTIONS = TopLevelNavItem(
    selectedIcon = ImageVectorIcon(PhovoIcons.Dns),
    unselectedIcon = ImageVectorIcon(PhovoIcons.DnsBorder),
    iconTextId = connectionsRes.string.feature_connections_title,
    titleTextId = connectionsRes.string.feature_connections_title
)

val TOP_LEVEL_NAV_ITEMS: Map<NavKey, TopLevelNavItem> = mapOf(
    PhotosHomeNavKey to PHOTOS,
    SearchHomeNavKey to SEARCH,
    ConnectionsRouteComponent to CONNECTIONS
)