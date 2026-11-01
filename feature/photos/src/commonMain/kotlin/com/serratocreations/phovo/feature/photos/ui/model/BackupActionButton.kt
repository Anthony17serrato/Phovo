package com.serratocreations.phovo.feature.photos.ui.model

import org.jetbrains.compose.resources.StringResource

data class BackupActionButton(
    /** The text to be displayed on the button for the action */
    val actionText: StringResource,
    /** What tapping the button does */
    val action: BackupStatusAction
)

/**
 * What tapping a backup status action does, as data rather than a lambda. The view model cannot
 * navigate, so the UI reads this and routes each case to whoever owns it.
 */
sealed interface BackupStatusAction {
    /** Ask for gallery access, or open the system settings once the app can no longer ask. */
    data object AllowPhotoAccess : BackupStatusAction

    /** Take the user to where a server is set up. Navigation, so the UI layer handles it. */
    data object SetUpServer : BackupStatusAction

    /** Show the items the last backup could not upload. */
    data object ViewFailedItems : BackupStatusAction
}
