package com.serratocreations.phovo.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.feature.photos.navigation.PhotosRoute
import com.serratocreations.phovo.feature.connections.navigation.ConnectionsRoute
import phovo.composeapp.generated.resources.Res
import phovo.composeapp.generated.resources.feature_bookmarks_title
import org.jetbrains.compose.resources.StringResource
import phovo.composeapp.generated.resources.app_name
import phovo.feature.photos.generated.resources.feature_photos_title
import phovo.feature.photos.generated.resources.Res as photosRes
import phovo.feature.connections.generated.resources.Res as connectionsRes
import phovo.feature.connections.generated.resources.feature_connections_title
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: StringResource,
    val titleTextId: StringResource,
    val route: KClass<*>,
) {
    PHOTOS(
        selectedIcon = PhovoIcons.Photo,
        unselectedIcon = PhovoIcons.PhotoBorder,
        iconTextId = photosRes.string.feature_photos_title,
        titleTextId = Res.string.app_name,
        route = PhotosRoute::class,
    ),
    BOOKMARKS(
        selectedIcon = PhovoIcons.Bookmarks,
        unselectedIcon = PhovoIcons.BookmarksBorder,
        iconTextId = Res.string.feature_bookmarks_title,
        titleTextId = Res.string.feature_bookmarks_title,
        route = BookmarksRoute::class,
    ),
    Connections(
        selectedIcon = PhovoIcons.Dns,
        unselectedIcon = PhovoIcons.DnsBorder,
        iconTextId = connectionsRes.string.feature_connections_title,
        titleTextId = connectionsRes.string.feature_connections_title,
        route = ConnectionsRoute::class,
    ),
}