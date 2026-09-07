package com.serratocreations.phovo.core.model.network

sealed interface NetworkResult<out T> {
    data class NetworkSuccess<out T>(val data: T) : NetworkResult<T>

    data class NetworkError(
        val message: String? = null,
        val failure: NetworkFailure = NetworkFailure.Unknown
    ): NetworkResult<Nothing>
}

/**
 * Machine-readable classification of a failed network call.
 *
 * [NetworkResult.NetworkError.message] stays a human-readable description; anything that needs to
 * branch on the *kind* of failure — retry policy, whether to go looking for the server somewhere
 * else, what to tell the user — should use this rather than parsing that string.
 */
enum class NetworkFailure {
    /** The server could not be contacted at all: refused, host down, address unresolvable. */
    Unreachable,

    /** The connection or request exceeded its timeout. */
    Timeout,

    /** The server answered, but with a non 2xx status. */
    HttpStatus,

    /** The server answered successfully but the body could not be parsed. */
    Malformed,

    /** Unclassified. [NetworkResult.NetworkError.message] carries the exception type. */
    Unknown
}
