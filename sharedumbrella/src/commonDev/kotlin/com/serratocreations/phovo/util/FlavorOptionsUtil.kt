package com.serratocreations.phovo.util

import com.serratocreations.phovo.navigation.DevMenuHomeNavKey
import com.serratocreations.phovo.ui.model.OverflowMenuOption
import phovo.sharedumbrella.generated.resources.Res
import phovo.sharedumbrella.generated.resources.dev_menu_option

fun getFlavorOptions(): Set<OverflowMenuOption> {
    return setOf(
        OverflowMenuOption(
            title = Res.string.dev_menu_option,
            route = DevMenuHomeNavKey
        )
    )
}