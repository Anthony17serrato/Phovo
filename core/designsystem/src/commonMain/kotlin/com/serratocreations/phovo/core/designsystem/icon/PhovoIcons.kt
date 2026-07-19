package com.serratocreations.phovo.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource
import phovo.core.designsystem.generated.resources.*

object PhovoIcons {
    val Add: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_add_rounded)

    val ArrowBack: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_arrow_back_rounded)

    val Search: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_search_rounded)

    val SearchBorder: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_search_outlined)

    val Check: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_check_rounded)

    val Close: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_close_rounded)

    val Person: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_person_rounded)

    val More: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_more_vert_rounded)

    val ShortText: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_short_text_rounded)

    val Photo: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_photo_rounded)

    val PhotoBorder: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_photo_outlined)

    val Upcoming: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_upcoming_rounded)

    val UpcomingBorder: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_upcoming_outlined)

    val ViewDay: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_view_day_rounded)

    val Info: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_info_outlined)

    val ChevronRight: ImageVector
        @Composable
        get() = vectorResource(Res.drawable.ic_chevron_right_rounded)
}