package com.voicetel.sdk

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class TransportTest {

    @Test
    fun successUnwrapsEnvelope() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"name":"Acme","email":"a@b.com"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val data = client.account.get()
        assertEquals("Acme", data.name)
        assertEquals("a@b.com", data.email)
        client.close()
    }

    @Test
    fun successWithoutEnvelope() = runTest {
        val client = mockClient { _ ->
            respond("""{"name":"Bare","email":"b@b.com"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val data = client.account.get()
        assertEquals("Bare", data.name)
        client.close()
    }

    @Test
    fun error400MapsBadRequest() = runTest {
        val client = mockClient { _ ->
            respond("""{"error":"oops"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.BadRequest, err.kind)
        assertEquals(400, err.statusCode)
        client.close()
    }

    @Test
    fun error401IsAuthentication() = runTest {
        val client = mockClient { _ ->
            respond("""{"message":"no auth"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.Authentication, err.kind)
        assertTrue(err.isAuthentication)
        client.close()
    }

    @Test
    fun error403IsPermissionDenied() = runTest {
        val client = mockClient { _ ->
            respond("""{"error":"nope"}""", HttpStatusCode.Forbidden, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.PermissionDenied, err.kind)
        client.close()
    }

    @Test
    fun error404IsNotFound() = runTest {
        val client = mockClient { _ ->
            respond("""{"error":"missing"}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.numbers.get("9999999999") }
        assertTrue(err.isNotFound)
        client.close()
    }

    @Test
    fun error409IsConflictWithBody() = runTest {
        val client = mockClient { _ ->
            respond("""{"error":"conflict","data":{"failed":[]}}""", HttpStatusCode.Conflict, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertTrue(err.isConflict)
        assertNotNull(err.body)
        client.close()
    }

    @Test
    fun error500IsServer() = runTest {
        val client = mockClient { _ ->
            respond("""server error""", HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.Server, err.kind)
        client.close()
    }

    @Test
    fun rateLimit429RetriesThenSucceeds() = runTest {
        var calls = 0
        val client = mockClientRetries(maxRetries = 2) { _ ->
            calls++
            if (calls == 1) {
                respond(
                    """{"error":"rate limit"}""",
                    HttpStatusCode.TooManyRequests,
                    headersOf(HttpHeaders.RetryAfter to listOf("0")),
                )
            } else {
                respond(envelope("""{"name":"after-retry"}"""), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val data = client.account.get()
        assertEquals("after-retry", data.name)
        assertEquals(2, calls)
        client.close()
    }

    @Test
    fun retriesExhaustedThrowsRateLimit() = runTest {
        var calls = 0
        val client = mockClientRetries(maxRetries = 1) { _ ->
            calls++
            respond(
                """{"error":"rate"}""",
                HttpStatusCode.TooManyRequests,
                headersOf(HttpHeaders.RetryAfter to listOf("0")),
            )
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertTrue(err.isRateLimit)
        assertEquals(2, calls) // initial + 1 retry
        client.close()
    }

    @Test
    fun authMissingThrowsBeforeDispatch() = runTest {
        val client = mockClient(apiKey = "") { _ ->
            respond("", HttpStatusCode.OK)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.Authentication, err.kind)
        client.close()
    }

    @Test
    fun nonJsonErrorBodyStillRaisesApiError() = runTest {
        val client = mockClient { _ ->
            respond("plain text 500", HttpStatusCode.InternalServerError)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(ErrorKind.Server, err.kind)
        client.close()
    }

    @Test
    fun errorKindFromStatus() {
        assertEquals(ErrorKind.BadRequest, ErrorKind.fromStatus(400))
        assertEquals(ErrorKind.Authentication, ErrorKind.fromStatus(401))
        assertEquals(ErrorKind.PermissionDenied, ErrorKind.fromStatus(403))
        assertEquals(ErrorKind.NotFound, ErrorKind.fromStatus(404))
        assertEquals(ErrorKind.Conflict, ErrorKind.fromStatus(409))
        assertEquals(ErrorKind.RateLimit, ErrorKind.fromStatus(429))
        assertEquals(ErrorKind.Server, ErrorKind.fromStatus(503))
        assertEquals(ErrorKind.Unknown, ErrorKind.fromStatus(418))
    }

    @Test
    fun apiErrorFlags() {
        val a = ApiError("m", ErrorKind.RateLimit, 429)
        assertTrue(a.isRateLimit)
        val b = ApiError("m", ErrorKind.NotFound, 404)
        assertTrue(b.isNotFound)
        val c = ApiError("m", ErrorKind.Conflict, 409)
        assertTrue(c.isConflict)
    }

    @Test
    fun clientOptionsValidates() {
        assertFailsWith<IllegalArgumentException> { ClientOptions(maxRetries = -1) }
        assertFailsWith<IllegalArgumentException> { ClientOptions(timeoutMillis = 0) }
    }

    @Test
    fun versionConstantsPresent() {
        assertEquals("2.2.10", Version.SDK_VERSION)
        assertEquals("v2.2.10", Version.API_VERSION)
        assertTrue(Version.DEFAULT_BASE_URL.startsWith("https://"))
    }

    @Test
    fun clientBuilderDsl() {
        val client = VoiceTelClient {
            apiKey = "abc"
            baseUrl = "https://api.example.com/"
            timeoutMillis = 10_000
            maxRetries = 0
        }
        assertEquals("abc", client.apiKey)
        // trailing slash stripped
        assertEquals("https://api.example.com", client.baseUrl)
        client.close()
    }

    @Test
    fun loginInstallsBearer() = runTest {
        val client = mockClient(apiKey = "") { req ->
            // login path doesn't require auth
            assertEquals("/v2.2/account/api-key", req.url.encodedPath)
            respond(envelope("""{"apikey":"NEWKEY"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val key = client.login(1000000001, "hunter2")
        assertEquals("NEWKEY", key)
        assertEquals("NEWKEY", client.apiKey)
        client.close()
    }

    @Test
    fun loginEmptyKeyFails() = runTest {
        val client = mockClient(apiKey = "") { _ ->
            respond(envelope("""{"apikey":""}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.login(1, "x") }
        assertEquals(ErrorKind.Authentication, err.kind)
        client.close()
    }
}
