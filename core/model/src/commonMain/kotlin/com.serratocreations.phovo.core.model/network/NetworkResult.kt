package com.serratocreations.phovo.core.model.network

sealed interface NetworkResult<out T> {
    data class NetworkSuccess<out T>(val data: T) : NetworkResult<T>

    data class NetworkError(
        val message: String? = null,
        val failure: NetworkFailure = NetworkFailure.Unknown,
        val statusCode: Int? = null
    ): NetworkResult<Nothing>
}

/**
 * Machine readable classification of a failed network call. [NetworkResult.NetworkError.message]
 * remains a human readable description; callers that need to branch on the kind of failure should
 * use this instead of parsing that string.
 */
enum class NetworkFailure {
    /** The server could not be contacted at all — refused, host down, address unresolvable. */
    Unreachable,

    /** The connection or request exceeded its timeout. */
    Timeout,

    /** The server answered, but with a non 2xx status. See [NetworkResult.NetworkError.statusCode]. */
    HttpStatus,

    /** The server answered successfully but the body could not be parsed. */
    Malformed,

    /** Unclassified. Inspect [NetworkResult.NetworkError.message], which carries the exception type. */
    Unknown
}
