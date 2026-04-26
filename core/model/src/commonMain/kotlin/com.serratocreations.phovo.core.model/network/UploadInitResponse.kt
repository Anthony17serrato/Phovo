package com.serratocreations.phovo.core.model.network

import kotlinx.serialization.Serializable

@Serializable
data class UploadInitResponse(
    val uploadRequired: Boolean,
    val message: String
)