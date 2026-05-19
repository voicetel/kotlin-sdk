package com.voicetel.sdk

import com.voicetel.sdk.models.INumbering
import com.voicetel.sdk.resources.INumberingService
import com.voicetel.sdk.resources.MessagingService
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class EdgeCasesTest {

    // -- Coverage: error response with a structured `code` field --
    @Test
    fun errorBodyExtractsCode() = runTest {
        val client = mockClient { _ ->
            respond(
                """{"code":"E_BAD","message":"validation failed"}""",
                HttpStatusCode.BadRequest, jsonHeaders,
            )
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals("E_BAD", err.code)
        assertEquals("validation failed", err.message)
        client.close()
    }

    // -- Coverage: Retry-After invalid header value falls back to exponential backoff --
    @Test
    fun retryAfterMalformedFallback() = runTest {
        var calls = 0
        val client = mockClientRetries(maxRetries = 1) { _ ->
            calls++
            if (calls == 1) {
                respond(
                    """{"err":"x"}""",
                    HttpStatusCode.ServiceUnavailable,
                    headersOf(HttpHeaders.RetryAfter, "notanumber"),
                )
            } else {
                respond(envelope("""{"username":"u"}"""), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val d = client.account.get()
        assertEquals("u", d.username)
        client.close()
    }

    // -- Coverage: 500 with no Retry-After header --
    @Test
    fun serverErrorRetriesWithBackoff() = runTest {
        var calls = 0
        val client = mockClientRetries(maxRetries = 2) { _ ->
            calls++
            if (calls < 3) {
                respond("""{"err":"5xx"}""", HttpStatusCode.BadGateway, jsonHeaders)
            } else {
                respond(envelope("""{"username":"u"}"""), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val d = client.account.get()
        assertNotNull(d.username)
        assertEquals(3, calls)
        client.close()
    }

    // -- Coverage: INumbering search query passes all params --
    @Test
    fun inventoryFullQuery() = runTest {
        val client = mockClient { req ->
            val q = req.url.parameters
            assertEquals("201", q["npa"])
            assertEquals("555", q["nxx"])
            assertEquals("NJ", q["state"])
            assertEquals("Newark", q["ratecenter"])
            assertEquals("9999", q["contains"])
            assertEquals("1234", q["endswith"])
            assertEquals("10", q["limit"])
            respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.iNumbering.searchInventory(INumberingService.InventoryQuery(
            npa = 201, nxx = 555, state = "NJ", rateCenter = "Newark",
            contains = "9999", endsWith = "1234", limit = 10,
        ))
        client.close()
    }

    // -- Coverage: Coverage query with both fields --
    @Test
    fun coverageBothFields() = runTest {
        val client = mockClient { req ->
            assertEquals("NJ", req.url.parameters["state"])
            assertEquals("Newark", req.url.parameters["ratecenter"])
            respond(envelope("""{"coverage":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.iNumbering.coverage(INumberingService.CoverageQuery(state = "NJ", rateCenter = "Newark"))
        client.close()
    }

    // -- Coverage: numbersState with empty/null list omits the param --
    @Test
    fun numbersStateNullOmits() = runTest {
        val client = mockClient { req ->
            assertNull(req.url.parameters["numbers"])
            respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.numbersState(null)
        client.close()
    }

    @Test
    fun numbersStateEmptyListOmits() = runTest {
        val client = mockClient { req ->
            assertNull(req.url.parameters["numbers"])
            respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.numbersState(emptyList())
        client.close()
    }

    // -- Coverage: history options with all fields --
    @Test
    fun historyAllFields() = runTest {
        val client = mockClient { req ->
            val q = req.url.parameters
            assertEquals("2015551234", q["number"])
            assertEquals("1", q["start"])
            assertEquals("2", q["end"])
            assertEquals("sms", q["type"])
            respond(envelope("""{"messages":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.history(MessagingService.HistoryOptions(
            number = "2015551234", start = 1, end = 2, type = "sms",
        ))
        client.close()
    }

    // -- Coverage: OrderNumber serializer encodes both forms --
    @Test
    fun orderNumberSerializerEncodesBothShapes() {
        val json = Json
        val plainStr = json.encodeToString(INumbering.OrderNumber.serializer(), INumbering.OrderNumber.of("2015551234"))
        assertEquals(""""2015551234"""", plainStr)
        val objStr = json.encodeToString(INumbering.OrderNumber.serializer(), INumbering.OrderNumber.of("2015551234", 7))
        assertTrue(objStr.contains("\"number\":\"2015551234\""))
        assertTrue(objStr.contains("\"route\":7"))
    }

    @Test
    fun orderNumberSerializerDecodesBothShapes() {
        val json = Json
        val plain = json.decodeFromString(INumbering.OrderNumber.serializer(), """"2015551234"""")
        assertEquals(INumbering.OrderNumber.of("2015551234"), plain)
        val obj = json.decodeFromString(INumbering.OrderNumber.serializer(),
            """{"number":"2015551234","route":7}""")
        assertEquals(INumbering.OrderNumber.of("2015551234", 7), obj)
    }

    @Test
    fun orderNumberOfWithNullRouteUsesPlain() {
        val plain = INumbering.OrderNumber.of("2015551234", null)
        assertEquals(INumbering.OrderNumber.of("2015551234"), plain)
    }

    // -- Coverage: Account add KDoc convenience defaults --
    @Test
    fun accountAddDefaultMasterAccount() {
        val r = com.voicetel.sdk.models.Account.AddRequest(
            username = 1000000002, name = "x", email = "y")
        assertEquals(null, r.masterAccount)
    }

    // -- Coverage: client.baseUrl with no trailing slash already --
    @Test
    fun baseUrlNoTrailingSlashUnchanged() {
        val client = VoiceTelClient {
            apiKey = "x"
            baseUrl = "https://example.com"
        }
        assertEquals("https://example.com", client.baseUrl)
        client.close()
    }

    // -- Coverage: ApiError without body or code --
    @Test
    fun apiErrorPlainErrorString() = runTest {
        val client = mockClient { _ ->
            respond("not json at all", HttpStatusCode.BadRequest, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(400, err.statusCode)
        client.close()
    }

    // -- Coverage: Version object instantiation defaults --
    @Test
    fun versionObjectAccessors() {
        assertEquals("2.2.10", Version.SDK_VERSION)
        assertEquals("v2.2.10", Version.API_VERSION)
        assertTrue(Version.DEFAULT_USER_AGENT.contains("kotlin"))
        assertTrue(Version.DEFAULT_USER_AGENT.contains("2.2.10"))
    }

    // -- Coverage: ClientOptions defaults companion --
    @Test
    fun clientOptionsDefaultsCompanion() {
        val o = ClientOptions.defaults()
        assertEquals("", o.apiKey)
        assertEquals(2, o.maxRetries)
    }

    // -- Coverage: AuthenticationService constants accessible --
    @Test
    fun authenticationTypeConstants() {
        assertEquals(0, com.voicetel.sdk.models.Authentication.TYPE_DIGEST)
        assertEquals(1, com.voicetel.sdk.models.Authentication.TYPE_IP_AUTH)
        assertEquals(2, com.voicetel.sdk.models.Authentication.TYPE_DIGEST_OR_IP)
        assertEquals(3, com.voicetel.sdk.models.Authentication.TYPE_DIGEST_AND_IP)
    }
}
