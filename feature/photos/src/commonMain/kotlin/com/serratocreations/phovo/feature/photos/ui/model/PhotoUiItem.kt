package com.serratocreations.phovo.feature.photos.ui.model

sealed interface PhotoUiItem {
    /** Unique identifier for each PhotoUiItem */
    val key: String
}