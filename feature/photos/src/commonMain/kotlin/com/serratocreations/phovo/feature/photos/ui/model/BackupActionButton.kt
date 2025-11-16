package com.serratocreations.phovo.feature.photos.ui.model

import org.jetbrains.compose.resources.StringResource

data class BackupActionButton(
    /** The text to be displayed on the button for the action */
    val actionText: StringResource,
    /** The action to execute on click of the action button */
    val onActionButtonClick: () -> Unit
)