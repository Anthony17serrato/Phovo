package com.serratocreations.phovo.core.model.network

sealed interface NetworkResult<out T> {
    data class NetworkSuccess<out T>(val data: T) : NetworkResult<T>

    data class NetworkError(
        val message: String? = null
    ): NetworkResult<Nothing>
}