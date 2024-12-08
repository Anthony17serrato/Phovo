package com.serratocreations.phovo.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import com.serratocreations.phovo.feature.photos.navigation.PhotosRoute
import phovo.composeapp.generated.resources.Res
import phovo.composeapp.generated.resources.feature_bookmarks_title
import phovo.composeapp.generated.resources.feature_search_interests
import org.jetbrains.compose.resources.StringResource
import phovo.composeapp.generated.resources.app_name
import phovo.feature.photos.generated.resources.feature_photos_title
import phovo.feature.photos.generated.resources.Res as photosRes
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
    INTERESTS(
        selectedIcon = PhovoIcons.Grid3x3,
        unselectedIcon = PhovoIcons.Grid3x3,
        iconTextId = Res.string.feature_search_interests,
        titleTextId = Res.string.feature_search_interests,
        route = InterestsRoute::class,
    ),
}