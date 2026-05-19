package com.voicetel.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Build a [VoiceTelClient] backed by a MockEngine for tests. */
internal fun mockClient(
    apiKey: String = "TESTKEY",
    handler: MockRequestHandler,
): VoiceTelClient {
    val engine = MockEngine { req -> handler(req) }
    val httpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
        expectSuccess = false
    }
    return VoiceTelClient(
        ClientOptions(
            apiKey = apiKey,
            baseUrl = "https://api.test.local",
            maxRetries = 1,
            httpClient = httpClient,
        ),
    )
}

/** Build a [VoiceTelClient] with retries configurable. */
internal fun mockClientRetries(
    maxRetries: Int,
    apiKey: String = "TESTKEY",
    handler: MockRequestHandler,
): VoiceTelClient {
    val engine = MockEngine { req -> handler(req) }
    val httpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
        expectSuccess = false
    }
    return VoiceTelClient(
        ClientOptions(
            apiKey = apiKey,
            baseUrl = "https://api.test.local",
            maxRetries = maxRetries,
            httpClient = httpClient,
        ),
    )
}

/** Convenience: build a 200 response with a JSON body wrapped in the API envelope. */
internal fun envelope(data: String): String = """{"status":"success","data":$data}"""

internal val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
