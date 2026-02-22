package com.serratocreations.phovo.feature.photos.ui.model

import com.serratocreations.phovo.core.common.util.localize
import com.serratocreations.phovo.core.designsystem.PhovoPlural
import com.serratocreations.phovo.core.designsystem.PhovoString
import com.serratocreations.phovo.core.designsystem.UiStringBuilder
import org.jetbrains.compose.resources.StringResource
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.chip_backup_complete
import phovo.feature.photos.generated.resources.chip_preparing_backup
import phovo.feature.photos.generated.resources.chip_backup_in_progress
import phovo.feature.photos.generated.resources.chip_server_offline
import phovo.feature.photos.generated.resources.main_status_complete
import phovo.feature.photos.generated.resources.main_status_scanning
import phovo.feature.photos.generated.resources.header_status_in_progress
import phovo.feature.photos.generated.resources.description_backup_complete
import phovo.feature.photos.generated.resources.description_server_offline
import phovo.feature.photos.generated.resources.description_sync_tip
import phovo.feature.photos.generated.resources.header_server_offline
import phovo.feature.photos.generated.resources.main_status_in_progress
import phovo.feature.photos.generated.resources.main_status_server_offline

sealed interface BackupStatusUiModel {
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

data object ServerOfflineUiModel: BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_server_offline
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.header_server_offline))
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_server_offline))
    override val statusDescription: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.description_server_offline))
    override val actionButton: BackupActionButton? = null
}

data object PreparingBackupUiModel: BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_preparing_backup
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.chip_preparing_backup))
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_scanning))
    override val statusDescription: UiStringBuilder? = null
    override val actionButton: BackupActionButton? = null
}

data class BackupInProgressUiModel(
    private val syncedCount: Int,
    private val totalCount: Int
): BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_backup_in_progress
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoPlural(Res.plurals.header_status_in_progress, totalCount),
            totalCount.localize())
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_in_progress),
        syncedCount.localize(), totalCount.localize())
    override val statusDescription: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.description_sync_tip))
    override val actionButton: BackupActionButton? = null
    val progress = syncedCount.toFloat()/totalCount.toFloat()
}

data class BackupCompleteUiModel(
    private val backedUpQuantity: Long,
    private val failureQuantity: Long,
    override val actionButton: BackupActionButton
): BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_backup_complete
    override val header: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.chip_backup_complete))
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_complete),
        backedUpQuantity.localize())
    override val statusDescription: UiStringBuilder = UiStringBuilder(
        resource = PhovoPlural(Res.plurals.description_backup_complete, failureQuantity.toInt()),
        failureQuantity.localize()
    )
}