package com.voicetel.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/**
 * Construction-time configuration for [VoiceTelClient].
 *
 * Use the DSL builder:
 * ```
 * val client = VoiceTelClient {
 *     apiKey = System.getenv("VOICETEL_API_KEY")
 *     timeoutMillis = 30_000
 *     maxRetries = 2
 * }
 * ```
 */
public class ClientOptions(
    /** Existing bearer token. Omit and call [VoiceTelClient.login]. */
    public var apiKey: String = "",
    /** Base URL override. Defaults to [Version.DEFAULT_BASE_URL]. */
    public var baseUrl: String = Version.DEFAULT_BASE_URL,
    /** Per-request timeout in milliseconds. Defaults to 30 seconds. */
    public var timeoutMillis: Long = 30_000L,
    /** How many times to retry 429/5xx responses. Defaults to 2 (total attempts = N+1). */
    public var maxRetries: Int = 2,
    /** User-Agent header. */
    public var userAgent: String = Version.DEFAULT_USER_AGENT,
    /** Inject a pre-built Ktor [HttpClient]. Used primarily by tests. */
    public var httpClient: HttpClient? = null,
    /** Inject a Ktor engine factory (mutually exclusive with [httpClient]). */
    public var engine: HttpClientEngine? = null,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        require(timeoutMillis > 0) { "timeoutMillis must be > 0" }
    }

    public companion object {
        /** A blank options object using all defaults. */
        public fun defaults(): ClientOptions = ClientOptions()
    }
}
