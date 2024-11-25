package com.serratocreations.phovo.feature.photos.ui.model

import kotlinx.datetime.Month

data class DateHeaderPhotoUiItem(
    val month: Month,
    val year: String
) : PhotoUiItem
