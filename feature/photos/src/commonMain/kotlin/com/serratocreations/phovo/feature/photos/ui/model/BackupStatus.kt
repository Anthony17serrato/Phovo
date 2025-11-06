package com.serratocreations.phovo.feature.photos.ui.model

import androidx.compose.runtime.Composable
import com.serratocreations.phovo.core.common.util.localize
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.chip_backup_complete
import phovo.feature.photos.generated.resources.chip_preparing_backup
import phovo.feature.photos.generated.resources.chip_backup_in_progress
import phovo.feature.photos.generated.resources.main_status_complete
import phovo.feature.photos.generated.resources.main_status_scanning
import phovo.feature.photos.generated.resources.header_status_in_progress
import phovo.feature.photos.generated.resources.description_backup_complete
import phovo.feature.photos.generated.resources.description_sync_tip
import phovo.feature.photos.generated.resources.main_status_in_progress

sealed interface BackupStatus {
    /** The text which appears on the backup status chip, it is the main entry point for
     * viewing additional status details */
    val chipText: StringResource
    /** Leading text that appears above status card */
    val header: UiStringBuilder
    /** Backup status that makes up the main backup card text */
    val mainStatus: UiStringBuilder
    /** Description text that appears in the card below [mainStatus] */
    val statusDescription: UiStringBuilder?
    /** Optional action to execute, button not rendered if null */
    val actionButton: BackupActionButton?
}

data class BackupActionButton(
    /** The text to be displayed on the button for the action */
    val actionText: StringResource,
    /** The action to execute on click of the action button */
    val onActionButtonClick: () -> Unit
)

sealed interface StringBuilderResource
data class PhovoString(val stringResource: StringResource): StringBuilderResource
data class PhovoPlural(
    val pluralResource: PluralStringResource,
    val pluralCount: Int
): StringBuilderResource

/**
 * Given the provided constructor arguments this model is able to build strings correctly at the
 * time in which the UI needs them.
 * @param resource is the defined resource type
 * @param templateArgs any arguments that are necessary to build the string
 */
class UiStringBuilder(
    private val resource: StringBuilderResource,
    private vararg val templateArgs: String
) {
    /**
     * Build the string defined by this builder
     */
    @Composable
    fun build(): String {
        return when(resource) {
            is PhovoPlural -> {
                pluralStringResource(resource.pluralResource, resource.pluralCount,
                    *(templateArgs))
            }
            is PhovoString -> {
                stringResource(resource.stringResource, *(templateArgs))
            }
        }
    }
}

data object PreparingBackup: BackupStatus {
    override val chipText: StringResource = Res.string.chip_preparing_backup
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.chip_preparing_backup))
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_scanning))
    override val statusDescription: UiStringBuilder? = null
    override val actionButton: BackupActionButton? = null
}

data class BackupInProgress(
    private val syncedCount: Int,
    private val totalCount: Int
): BackupStatus {
    override val chipText: StringResource = Res.string.chip_backup_in_progress
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoPlural(Res.plurals.header_status_in_progress, totalCount),
            totalCount.localize())
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_in_progress),
        syncedCount.localize(), totalCount.localize())
    override val statusDescription: UiStringBuilder? = UiStringBuilder(
        PhovoString(Res.string.description_sync_tip))
    override val actionButton: BackupActionButton? = null
    val progress = syncedCount.toFloat()/totalCount.toFloat()
}

data class BackupComplete(
    private val backedUpQuantity: Long,
    private val failureQuantity: Long,
    override val actionButton: BackupActionButton
): BackupStatus {
    override val chipText: StringResource = Res.string.chip_backup_complete
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.chip_backup_complete))
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_complete),
        backedUpQuantity.localize())
    override val statusDescription: UiStringBuilder? = UiStringBuilder(
        resource = PhovoPlural(Res.plurals.description_backup_complete, failureQuantity.toInt()),
        failureQuantity.localize()
    )
}