package com.serratocreations.phovo.core.model.network

/**
 * Normalises a user typed server address into `scheme://host[:port]`.
 *
 * Manual entry is free text, and [BaseUrl.div] is a plain concatenation, so an unnormalised value
 * fails later in ways that read as "the server is unreachable": a trailing slash produces `//` in
 * every request path, and a missing scheme produces a URL that never resolves.
 *
 * @return the normalised address, or null when [input] cannot be a server address.
 */
fun normalizeServerUrl(input: String): String? {
    val trimmed = input.trim().trimEnd('/')
    if (trimmed.isEmpty()) return null

    val schemeSeparator = trimmed.indexOf("://")
    val scheme: String
    val remainder: String
    if (schemeSeparator == -1) {
        scheme = "http"
        remainder = trimmed
    } else {
        scheme = trimmed.substring(0, schemeSeparator).lowercase()
        remainder = trimmed.substring(schemeSeparator + 3)
        if (scheme != "http" && scheme != "https") return null
    }

    // Anything past the authority is a path, which a base URL must not carry.
    val authority = remainder.substringBefore('/').substringBefore('?')
    if (authority.isEmpty()) return null

    val host: String
    val port: String?
    if (authority.startsWith("[")) {
        // Bracketed IPv6 literal, e.g. [fe80::1]:8080
        val closingBracket = authority.indexOf(']')
        if (closingBracket == -1) return null
        host = authority.substring(0, closingBracket + 1)
        val rest = authority.substring(closingBracket + 1)
        port = if (rest.startsWith(":")) rest.drop(1) else if (rest.isEmpty()) null else return null
    } else if (authority.count { it == ':' } > 1) {
        // Unbracketed IPv6 literal — bracket it so it is usable in a URL authority.
        host = "[$authority]"
        port = null
    } else {
        host = authority.substringBefore(':')
        port = authority.substringAfter(':', missingDelimiterValue = "").takeIf { it.isNotEmpty() }
    }

    if (host.isEmpty() || host == "[]") return null
    if (port != null && (port.toIntOrNull() ?: return null) !in 1..65535) return null

    return if (port == null) "$scheme://$host" else "$scheme://$host:$port"
}
