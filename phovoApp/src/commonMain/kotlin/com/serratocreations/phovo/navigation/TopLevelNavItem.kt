package com.serratocreations.phovo.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.core.navigation.PhotosHomeNavKey
import com.serratocreations.phovo.feature.connections.ui.ConnectionsRouteComponent
import phovo.phovoapp.generated.resources.Res
import phovo.phovoapp.generated.resources.feature_search_title
import org.jetbrains.compose.resources.StringResource
import phovo.phovoapp.generated.resources.app_name
import phovo.feature.photos.generated.resources.feature_photos_title
import phovo.feature.photos.generated.resources.Res as photosRes
import phovo.feature.connections.generated.resources.Res as connectionsRes
import phovo.feature.connections.generated.resources.feature_connections_title

data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: StringResource,
    val titleTextId: StringResource,
)

val PHOTOS = TopLevelNavItem(
    selectedIcon = PhovoIcons.Photo,
    unselectedIcon = PhovoIcons.PhotoBorder,
    iconTextId = photosRes.string.feature_photos_title,
    titleTextId = Res.string.app_name
)

val SEARCH = TopLevelNavItem(
    selectedIcon = PhovoIcons.Search,
    unselectedIcon = PhovoIcons.SearchBorder,
    iconTextId = Res.string.feature_search_title,
    titleTextId = Res.string.feature_search_title,
)

val CONNECTIONS = TopLevelNavItem(
    selectedIcon = PhovoIcons.Dns,
    unselectedIcon = PhovoIcons.DnsBorder,
    iconTextId = connectionsRes.string.feature_connections_title,
    titleTextId = connectionsRes.string.feature_connections_title
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    PhotosHomeNavKey to PHOTOS,
    SearchHomeNavKey to SEARCH,
    ConnectionsRouteComponent to CONNECTIONS
)