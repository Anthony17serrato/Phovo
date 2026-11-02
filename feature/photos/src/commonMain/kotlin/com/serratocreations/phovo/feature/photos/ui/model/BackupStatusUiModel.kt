package com.serratocreations.phovo.feature.photos.ui.model

import com.serratocreations.phovo.core.common.util.localize
import com.serratocreations.phovo.core.designsystem.PhovoPlural
import com.serratocreations.phovo.core.designsystem.PhovoString
import com.serratocreations.phovo.core.designsystem.UiStringBuilder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.button_allow_photo_access
import phovo.feature.photos.generated.resources.button_set_up_server
import phovo.feature.photos.generated.resources.button_view_failed_items
import phovo.feature.photos.generated.resources.chip_backup_complete
import phovo.feature.photos.generated.resources.chip_backup_failures
import phovo.feature.photos.generated.resources.chip_backup_in_progress
import phovo.feature.photos.generated.resources.chip_backup_off
import phovo.feature.photos.generated.resources.chip_backup_not_set_up
import phovo.feature.photos.generated.resources.chip_connecting
import phovo.feature.photos.generated.resources.chip_preparing_backup
import phovo.feature.photos.generated.resources.chip_server_offline
import phovo.feature.photos.generated.resources.description_backup_complete
import phovo.feature.photos.generated.resources.description_backup_not_set_up
import phovo.feature.photos.generated.resources.description_backup_not_set_up_processing
import phovo.feature.photos.generated.resources.description_backup_not_set_up_processing_count
import phovo.feature.photos.generated.resources.description_connecting_processing
import phovo.feature.photos.generated.resources.description_connecting_processing_count
import phovo.feature.photos.generated.resources.description_in_progress
import phovo.feature.photos.generated.resources.description_in_progress_processing
import phovo.feature.photos.generated.resources.description_in_progress_processing_count
import phovo.feature.photos.generated.resources.description_media_access_required
import phovo.feature.photos.generated.resources.description_preparing_backup
import phovo.feature.photos.generated.resources.description_preparing_backup_count
import phovo.feature.photos.generated.resources.description_server_offline
import phovo.feature.photos.generated.resources.description_server_offline_processing
import phovo.feature.photos.generated.resources.description_server_offline_processing_count
import phovo.feature.photos.generated.resources.header_backup_complete
import phovo.feature.photos.generated.resources.header_backup_in_progress
import phovo.feature.photos.generated.resources.header_backup_not_set_up
import phovo.feature.photos.generated.resources.header_connecting
import phovo.feature.photos.generated.resources.header_media_access_required
import phovo.feature.photos.generated.resources.header_preparing_backup
import phovo.feature.photos.generated.resources.header_server_offline
import phovo.feature.photos.generated.resources.ic_cloud_done_outlined
import phovo.feature.photos.generated.resources.ic_cloud_off_outlined
import phovo.feature.photos.generated.resources.main_status_backup_not_set_up
import phovo.feature.photos.generated.resources.main_status_complete
import phovo.feature.photos.generated.resources.main_status_connecting
import phovo.feature.photos.generated.resources.main_status_failures
import phovo.feature.photos.generated.resources.main_status_in_progress
import phovo.feature.photos.generated.resources.main_status_media_access_required
import phovo.feature.photos.generated.resources.main_status_preparing_backup
import phovo.feature.photos.generated.resources.main_status_server_offline

sealed interface BackupStatusUiModel {
    /** The text which appears on the backup status chip, it is the main entry point for
     * viewing additional status details */
    val chipText: StringResource

    /** What sits next to [chipText] and [mainStatus]: a spinner, a progress ring or an icon */
    val indicator: BackupStatusIndicator

    /** Leading text that appears above status card */
    val header: UiStringBuilder

    /** Backup status that makes up the main backup card text */
    val mainStatus: UiStringBuilder

    /** Description text that appears in the card below [mainStatus] */
    val statusDescription: UiStringBuilder?

    /** Optional action to execute, button not rendered if null */
    val actionButton: BackupActionButton?
}

/**
 * How far processing has got while it runs. [processedCount] counts every item handled, including
 * ones processed before, so it reaches [foundCount] when processing is done. [foundCount] is 0
 * until the scan has counted the library, and states fall back to their uncounted copy until then.
 */
data class ProcessingCounts(val processedCount: Int, val foundCount: Int)

sealed interface BackupStatusIndicator {
    /** Work is happening but there is nothing to measure it against yet */
    data object Indeterminate : BackupStatusIndicator

    data class Progress(val progress: Float) : BackupStatusIndicator

    data class Icon(val icon: DrawableResource) : BackupStatusIndicator
}

/** Phovo can't read the gallery, so nothing is backed up until the user allows access. */
data object MediaAccessRequiredUiModel : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_backup_off
    override val indicator: BackupStatusIndicator =
        BackupStatusIndicator.Icon(Res.drawable.ic_cloud_off_outlined)
    override val header: UiStringBuilder = Res.string.header_media_access_required.toUiString()
    override val mainStatus: UiStringBuilder =
        Res.string.main_status_media_access_required.toUiString()
    override val statusDescription: UiStringBuilder =
        Res.string.description_media_access_required.toUiString()
    override val actionButton: BackupActionButton = BackupActionButton(
        actionText = Res.string.button_allow_photo_access,
        action = BackupStatusAction.AllowPhotoAccess
    )
}

/**
 * Connecting to the server. When [processing], new media is being processed in the meantime, so
 * the description says so instead of leaving the card looking idle. The indicator stays a spinner
 * either way, since connecting is what the chip names.
 */
data class CheckingServerUiModel(
    private val processing: ProcessingCounts?
) : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_connecting
    override val indicator: BackupStatusIndicator = BackupStatusIndicator.Indeterminate
    override val header: UiStringBuilder = Res.string.header_connecting.toUiString()
    override val mainStatus: UiStringBuilder = Res.string.main_status_connecting.toUiString()
    override val statusDescription: UiStringBuilder? = processing?.description(
        uncounted = Res.string.description_connecting_processing,
        counted = Res.plurals.description_connecting_processing_count
    )
    override val actionButton: BackupActionButton? = null
}

/** No server has been set up, so nothing can be backed up until the user sets one up. */
data class BackupNotSetUpUiModel(
    private val processing: ProcessingCounts?
) : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_backup_not_set_up
    override val indicator: BackupStatusIndicator =
        processing.indicatorOr(Res.drawable.ic_cloud_off_outlined)
    override val header: UiStringBuilder = Res.string.header_backup_not_set_up.toUiString()
    override val mainStatus: UiStringBuilder = Res.string.main_status_backup_not_set_up.toUiString()
    override val statusDescription: UiStringBuilder = processing?.description(
        uncounted = Res.string.description_backup_not_set_up_processing,
        counted = Res.plurals.description_backup_not_set_up_processing_count
    ) ?: Res.string.description_backup_not_set_up.toUiString()
    override val actionButton: BackupActionButton = BackupActionButton(
        actionText = Res.string.button_set_up_server,
        action = BackupStatusAction.SetUpServer
    )
}

/** A server is set up but can't be reached, so backup is paused. */
data class ServerOfflineUiModel(
    private val processing: ProcessingCounts?
) : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_server_offline
    override val indicator: BackupStatusIndicator =
        processing.indicatorOr(Res.drawable.ic_cloud_off_outlined)
    override val header: UiStringBuilder = Res.string.header_server_offline.toUiString()
    override val mainStatus: UiStringBuilder = Res.string.main_status_server_offline.toUiString()
    override val statusDescription: UiStringBuilder = processing?.description(
        uncounted = Res.string.description_server_offline_processing,
        counted = Res.plurals.description_server_offline_processing_count
    ) ?: Res.string.description_server_offline.toUiString()
    override val actionButton: BackupActionButton? = null
}

/** Connected, but processing hasn't produced anything to upload yet. */
data class PreparingBackupUiModel(
    private val processing: ProcessingCounts
) : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_preparing_backup
    override val indicator: BackupStatusIndicator = processing.indicator()
    override val header: UiStringBuilder = Res.string.header_preparing_backup.toUiString()
    override val mainStatus: UiStringBuilder = Res.string.main_status_preparing_backup.toUiString()
    override val statusDescription: UiStringBuilder = processing.description(
        uncounted = Res.string.description_preparing_backup,
        counted = Res.plurals.description_preparing_backup_count
    )
    override val actionButton: BackupActionButton? = null
}

/**
 * Uploading. While [processing], [totalCount] keeps growing as new media is processed, so the
 * description warns that the total may go up rather than letting the progress look like it slipped.
 */
data class BackupInProgressUiModel(
    private val syncedCount: Int,
    private val totalCount: Int,
    private val processing: ProcessingCounts?
) : BackupStatusUiModel {
    override val chipText: StringResource = Res.string.chip_backup_in_progress
    // A sync can start with nothing pending, and 0 / 0 would be NaN.
    override val indicator: BackupStatusIndicator = BackupStatusIndicator.Progress(
        if (totalCount == 0) 0f else syncedCount.toFloat() / totalCount.toFloat()
    )
    override val header: UiStringBuilder = Res.string.header_backup_in_progress.toUiString()
    override val mainStatus: UiStringBuilder = UiStringBuilder(
        PhovoString(Res.string.main_status_in_progress),
        syncedCount.localize(), totalCount.localize())
    override val statusDescription: UiStringBuilder = processing?.description(
        uncounted = Res.string.description_in_progress_processing,
        counted = Res.plurals.description_in_progress_processing_count
    ) ?: Res.string.description_in_progress.toUiString()
    override val actionButton: BackupActionButton? = null
}

/**
 * The sync finished. [backedUpQuantity] only counts what this sync uploaded, so it is zero when
 * everything was already backed up, and the card says the library is backed up rather than
 * "0 items backed up".
 */
data class BackupCompleteUiModel(
    private val backedUpQuantity: Long,
    private val failureQuantity: Long
) : BackupStatusUiModel {
    private val hasFailures = failureQuantity > 0

    override val chipText: StringResource =
        if (hasFailures) Res.string.chip_backup_failures else Res.string.chip_backup_complete
    override val indicator: BackupStatusIndicator =
        BackupStatusIndicator.Icon(Res.drawable.ic_cloud_done_outlined)
    override val header: UiStringBuilder = Res.string.header_backup_complete.toUiString()
    override val mainStatus: UiStringBuilder = if (hasFailures) {
        UiStringBuilder(
            PhovoPlural(Res.plurals.main_status_failures, failureQuantity.toInt()),
            failureQuantity.localize()
        )
    } else {
        Res.string.main_status_complete.toUiString()
    }
    override val statusDescription: UiStringBuilder? = if (backedUpQuantity > 0) {
        UiStringBuilder(
            PhovoPlural(Res.plurals.description_backup_complete, backedUpQuantity.toInt()),
            backedUpQuantity.localize()
        )
    } else {
        null
    }
    override val actionButton: BackupActionButton? = if (hasFailures) {
        BackupActionButton(
            actionText = Res.string.button_view_failed_items,
            action = BackupStatusAction.ViewFailedItems
        )
    } else {
        null
    }
}

/** Processing progress, or a spinner until the scan has counted the library. */
private fun ProcessingCounts.indicator(): BackupStatusIndicator =
    if (foundCount == 0) {
        BackupStatusIndicator.Indeterminate
    } else {
        BackupStatusIndicator.Progress(
            (processedCount.toFloat() / foundCount.toFloat()).coerceAtMost(1f)
        )
    }

/** For a state without sync progress: processing progress while it runs, otherwise [idleIcon]. */
private fun ProcessingCounts?.indicatorOr(idleIcon: DrawableResource): BackupStatusIndicator =
    this?.indicator() ?: BackupStatusIndicator.Icon(idleIcon)

/** [counted] once the scan has counted the library, [uncounted] until then. */
private fun ProcessingCounts.description(
    uncounted: StringResource,
    counted: PluralStringResource
): UiStringBuilder =
    if (foundCount == 0) {
        uncounted.toUiString()
    } else {
        UiStringBuilder(
            PhovoPlural(counted, foundCount),
            processedCount.localize(), foundCount.localize()
        )
    }

private fun StringResource.toUiString(): UiStringBuilder = UiStringBuilder(PhovoString(this))
