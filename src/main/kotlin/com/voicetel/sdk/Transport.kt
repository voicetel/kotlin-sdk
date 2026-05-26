package com.voicetel.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min

/**
 * Internal HTTP transport. Owns the [HttpClient], the bearer token, the retry
 * policy, and JSON serialization.
 *
 * Resource services call through it; applications should configure it
 * indirectly via [ClientOptions] when constructing a [VoiceTelClient].
 */
public class Transport internal constructor(opts: ClientOptions) {

    @Volatile
    private var apiKey: String = opts.apiKey
    private val baseUrl: String = stripTrailingSlash(opts.baseUrl)
    private val maxRetries: Int = opts.maxRetries
    private val userAgent: String = opts.userAgent
    private val timeoutMillis: Long = opts.timeoutMillis
    private val ownsClient: Boolean
    private val httpClient: HttpClient

    /** JSON codec used for both wire framing and tests. */
    public val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = true
    }

    init {
        val provided = opts.httpClient
        if (provided != null) {
            ownsClient = false
            httpClient = provided
        } else {
            ownsClient = true
            httpClient = if (opts.engine != null) {
                HttpClient(opts.engine!!) { configure(this) }
            } else {
                HttpClient(CIO) { configure(this) }
            }
        }
    }

    private fun configure(builder: io.ktor.client.HttpClientConfig<*>) {
        builder.install(ContentNegotiation) {
            json(json)
        }
        builder.install(ContentEncoding) {
            gzip()
        }
        builder.install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = timeoutMillis
        }
        builder.expectSuccess = false
    }

    public fun getApiKey(): String = apiKey
    public fun getBaseUrl(): String = baseUrl
    public fun setBearer(key: String) { apiKey = key }

    /** Close the underlying [HttpClient] if it was created by this transport. */
    public fun close() {
        if (ownsClient) httpClient.close()
    }

    /**
     * Execute a request. Returns the parsed body decoded with [responseSerializer],
     * stripping the `{"status":"success","data": ...}` envelope when present.
     *
     * [bodySerializer] + [bodyValue] are the typed request body, or both null
     * if no body is to be sent.
     */
    public suspend fun <T> request(
        method: HttpMethod,
        path: String,
        query: Map<String, Any?>? = null,
        bodySerializer: SerializationStrategy<Any>? = null,
        bodyValue: Any? = null,
        responseSerializer: DeserializationStrategy<T>,
        requireAuth: Boolean = true,
    ): T {
        val resp = sendWithRetry(method, path, query, bodySerializer, bodyValue, requireAuth)
        return decodeBody(resp, responseSerializer)
    }

    /**
     * 204-style endpoint (no response body). Returns Unit on 2xx.
     */
    public suspend fun requestNoBody(
        method: HttpMethod,
        path: String,
        bodySerializer: SerializationStrategy<Any>? = null,
        bodyValue: Any? = null,
        requireAuth: Boolean = true,
    ) {
        val resp = sendWithRetry(method, path, null, bodySerializer, bodyValue, requireAuth)
        if (!resp.status.value.let { it in 200..299 }) {
            // Already thrown by sendWithRetry; defensive.
            throwApiError(resp)
        }
    }

    private suspend fun sendWithRetry(
        method: HttpMethod,
        path: String,
        query: Map<String, Any?>?,
        bodySerializer: SerializationStrategy<Any>?,
        bodyValue: Any?,
        requireAuth: Boolean,
    ): HttpResponse {
        if (requireAuth && apiKey.isEmpty()) {
            throw ApiError(
                "no api key set; pass apiKey to ClientOptions or call client.login()",
                kind = ErrorKind.Authentication,
            )
        }

        val serializedBody: String? = if (bodyValue != null && bodySerializer != null) {
            @Suppress("UNCHECKED_CAST")
            json.encodeToString(bodySerializer as SerializationStrategy<Any?>, bodyValue)
        } else null

        val idempotencyKey = if (method in setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch))
            java.util.UUID.randomUUID().toString() else null

        var lastError: Throwable? = null
        var attempt = 0
        while (attempt <= maxRetries) {
            val response: HttpResponse = try {
                httpClient.request {
                    this.method = method
                    url {
                        takeFrom(baseUrl + path)
                        query?.forEach { (k, v) -> if (v != null) parameters.append(k, v.toString()) }
                    }
                    header(HttpHeaders.UserAgent, userAgent)
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    if (requireAuth) header(HttpHeaders.Authorization, "Bearer $apiKey")
                    if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
                    if (serializedBody != null) {
                        contentType(ContentType.Application.Json)
                        setBody(serializedBody)
                    }
                }
            } catch (ex: Throwable) {
                lastError = ex
                if (attempt >= maxRetries) {
                    throw ApiError(
                        "transport error after ${attempt + 1} attempt(s): ${ex.message}",
                        kind = ErrorKind.Unknown,
                        cause = ex,
                    )
                }
                delay(backoffMillis(attempt, null))
                attempt++
                continue
            }

            val status = response.status.value
            if (status in RETRYABLE && attempt < maxRetries) {
                delay(backoffMillis(attempt, response))
                attempt++
                continue
            }

            if (status in 200..299) return response
            throwApiError(response)
        }
        throw ApiError("retry loop exhausted", kind = ErrorKind.Unknown, cause = lastError)
    }

    private suspend fun <T> decodeBody(resp: HttpResponse, deserializer: DeserializationStrategy<T>): T {
        val text = resp.bodyAsText()
        if (text.isEmpty()) {
            // Try to decode an empty object; if that fails, throw.
            return try {
                json.decodeFromString(deserializer, "{}")
            } catch (ex: Throwable) {
                throw ApiError(
                    "empty response body for non-Unit return type",
                    kind = ErrorKind.Unknown,
                    statusCode = resp.status.value,
                    cause = ex,
                )
            }
        }

        val root: JsonElement = try {
            json.parseToJsonElement(text)
        } catch (ex: Throwable) {
            throw ApiError(
                "non-JSON success response: ${truncate(text)}",
                kind = ErrorKind.Unknown,
                statusCode = resp.status.value,
                cause = ex,
            )
        }

        val payload: JsonElement = if (root is JsonObject && "status" in root && "data" in root) {
            root["data"]!!
        } else {
            root
        }

        return try {
            json.decodeFromJsonElement(deserializer, payload)
        } catch (ex: Throwable) {
            throw ApiError(
                "decode response body: ${ex.message}",
                kind = ErrorKind.Unknown,
                statusCode = resp.status.value,
                body = payload,
                cause = ex,
            )
        }
    }

    private suspend fun throwApiError(resp: HttpResponse): Nothing {
        val status = resp.status.value
        val text = runCatching { resp.bodyAsText() }.getOrDefault("")

        var bodyJson: JsonElement? = null
        var code: String? = null
        var message = "HTTP $status"
        if (text.isNotEmpty()) {
            runCatching {
                val root = json.parseToJsonElement(text)
                bodyJson = root
                if (root is JsonObject) {
                    val c = root["code"] ?: root["error"]
                    if (c is JsonPrimitive && c.isString) code = c.content
                    val m = root["message"] ?: root["error"]
                    if (m is JsonPrimitive && m.isString) message = m.content
                }
            }
        }

        throw ApiError(
            message = message,
            kind = ErrorKind.fromStatus(status),
            statusCode = status,
            code = code,
            body = bodyJson,
        )
    }

    private fun backoffMillis(attempt: Int, resp: HttpResponse?): Long {
        if (resp != null) {
            val ra = resp.headers[HttpHeaders.RetryAfter]
            if (ra != null) {
                val secs = ra.trim().toLongOrNull()
                if (secs != null && secs >= 0) return secs * 1000L
            }
        }
        val base = 500L
        val delay = base shl attempt
        return min(delay, 8_000L)
    }

    private companion object {
        val RETRYABLE = setOf(429, 500, 502, 503, 504)

        fun stripTrailingSlash(s: String?): String {
            if (s.isNullOrEmpty()) return ""
            var end = s.length
            while (end > 0 && s[end - 1] == '/') end--
            return s.substring(0, end)
        }

        fun truncate(s: String): String = if (s.length > 200) s.substring(0, 200) else s
    }
}
