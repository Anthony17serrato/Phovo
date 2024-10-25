package com.serratocreations.kanbanboard.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import kanbanboard.composeapp.generated.resources.Res
import kanbanboard.composeapp.generated.resources.app_name
import kanbanboard.composeapp.generated.resources.feature_bookmarks_title
import kanbanboard.composeapp.generated.resources.feature_search_interests
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: StringResource,
    val titleTextId: StringResource,
    val route: KClass<*>,
) {
    FOR_YOU(
        selectedIcon = PhovoIcons.Upcoming,
        unselectedIcon = PhovoIcons.UpcomingBorder,
        iconTextId = Res.string.app_name,
        titleTextId = Res.string.app_name,
        route = ForYouRoute::class,
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