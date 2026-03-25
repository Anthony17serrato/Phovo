package com.serratocreations.phovo.feature.photos.ui.model

import kotlinx.datetime.Month
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class DateHeaderPhotoUiItem(
    val month: Month,
    val year: Int?
) : PhotoUiItem {
    @OptIn(ExperimentalUuidApi::class)
    override val key: String = Uuid.random().toString()
}
