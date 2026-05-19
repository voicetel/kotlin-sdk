package com.voicetel.sdk

import kotlinx.serialization.json.JsonElement

/**
 * Classifies a VoiceTel API failure so callers can switch on it without
 * having to inspect raw HTTP status codes.
 */
public enum class ErrorKind {
    /** Catch-all for unmapped statuses or transport failures. */
    Unknown,

    /** HTTP 400 — server-side validation failure. */
    BadRequest,

    /** HTTP 401 — bearer token is missing, expired, or invalid. */
    Authentication,

    /** HTTP 403 — authenticated but not allowed. */
    PermissionDenied,

    /** HTTP 404 — resource does not exist. */
    NotFound,

    /** HTTP 409 — request conflicts with current state. */
    Conflict,

    /** HTTP 429 — exceeded the 6/hr/IP cap on account endpoints. */
    RateLimit,

    /** Any HTTP 5xx. */
    Server,
    ;

    public companion object {
        public fun fromStatus(status: Int): ErrorKind = when (status) {
            400 -> BadRequest
            401 -> Authentication
            403 -> PermissionDenied
            404 -> NotFound
            409 -> Conflict
            429 -> RateLimit
            in 500..599 -> Server
            else -> Unknown
        }
    }
}

/**
 * Thrown whenever the VoiceTel API responds with a non-2xx status, or when
 * the underlying HTTP layer fails before a response is received.
 *
 * For non-2xx responses, [body] carries the parsed JSON payload as a
 * [JsonElement] (object, array, or primitive). Useful for 409 conflicts
 * where the server returns structured detail about partial successes.
 */
public class ApiError(
    message: String,
    public val kind: ErrorKind = ErrorKind.Unknown,
    public val statusCode: Int = 0,
    public val code: String? = null,
    public val body: JsonElement? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /** True when this error is a [ErrorKind.RateLimit]. */
    public val isRateLimit: Boolean get() = kind == ErrorKind.RateLimit

    /** True when this error is a [ErrorKind.NotFound]. */
    public val isNotFound: Boolean get() = kind == ErrorKind.NotFound

    /** True when this error is an [ErrorKind.Authentication]. */
    public val isAuthentication: Boolean get() = kind == ErrorKind.Authentication

    /** True when this error is a [ErrorKind.Conflict]. */
    public val isConflict: Boolean get() = kind == ErrorKind.Conflict

    public companion object {
        private const val serialVersionUID: Long = 1L
    }
}
