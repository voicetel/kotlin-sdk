package com.voicetel.sdk

import com.voicetel.sdk.models.Account
import com.voicetel.sdk.models.INumbering
import com.voicetel.sdk.models.Numbers
import com.voicetel.sdk.resources.INumberingService
import com.voicetel.sdk.resources.MessagingService
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class EdgeCasesTest {

    // -- Coverage: spec error envelope carries nested numeric code + message --
    @Test
    fun errorBodyExtractsCode() = runTest {
        val client = mockClient { _ ->
            respond(
                """{"status":"error","error":{"code":400,"message":"Field Required: number"}}""",
                HttpStatusCode.BadRequest, jsonHeaders,
            )
        }
        val err = assertFailsWith<ApiError> { client.account.get() }
        assertEquals(400L, err.code)
        assertEquals("Field Required: number", err.message)
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

    // -- Coverage: INumbering search query passes all spec params --
    @Test
    fun inventoryFullQuery() = runTest {
        val client = mockClient { req ->
            val q = req.url.parameters
            assertEquals("201555*", q["tnMask"])
            assertEquals("25", q["quantity"])
            assertEquals("NJ", q["province"])
            assertEquals("Newark", q["rateCenter"])
            assertEquals("220", q["lata"])
            assertNull(q["npa"])
            assertNull(q["nxx"])
            assertNull(q["state"])
            assertNull(q["contains"])
            assertNull(q["endswith"])
            assertNull(q["limit"])
            respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.iNumbering.searchInventory(INumberingService.InventoryQuery(
            tnMask = "201555*", quantity = 25, province = "NJ", rateCenter = "Newark", lata = "220",
        ))
        client.close()
    }

    // -- Coverage: Coverage query sends countBy plus the spec filters --
    @Test
    fun coverageBothFields() = runTest {
        val client = mockClient { req ->
            assertEquals("npanxx", req.url.parameters["countBy"])
            assertEquals("NJ", req.url.parameters["locState"])
            assertEquals("Newark", req.url.parameters["city"])
            assertNull(req.url.parameters["state"])
            assertNull(req.url.parameters["ratecenter"])
            respond(envelope("""{"coverage":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.iNumbering.coverage(INumberingService.CoverageQuery(
            countBy = INumberingService.CountBy.NPANXX, locState = "NJ", city = "Newark",
        ))
        client.close()
    }

    // -- Coverage: numbersState always sends the required numbers param --
    @Test
    fun numbersStateSendsRequiredParam() = runTest {
        val client = mockClient { req ->
            assertEquals("2015550123,2015551235", req.url.parameters["numbers"])
            respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.numbersState(listOf("2015550123", "2015551235"))
        client.close()
    }

    // -- Coverage: history options with all fields (from/to are Unix timestamps) --
    @Test
    fun historyAllFields() = runTest {
        val client = mockClient { req ->
            val q = req.url.parameters
            assertEquals("2015550123", q["number"])
            assertEquals("1", q["from"])
            assertEquals("2", q["to"])
            assertEquals("sms", q["type"])
            assertNull(q["start"])
            assertNull(q["end"])
            respond(envelope("""{"messages":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.history(MessagingService.HistoryOptions(
            number = "2015550123", from = 1, to = 2, type = "sms",
        ))
        client.close()
    }

    // -- Coverage: OrderNumber serializer encodes both forms --
    @Test
    fun orderNumberSerializerEncodesBothShapes() {
        val json = Json
        val plainStr = json.encodeToString(INumbering.OrderNumber.serializer(), INumbering.OrderNumber.of("2015550123"))
        assertEquals(""""2015550123"""", plainStr)
        val objStr = json.encodeToString(INumbering.OrderNumber.serializer(), INumbering.OrderNumber.of("2015550123", 4))
        assertTrue(objStr.contains("\"number\":\"2015550123\""))
        assertTrue(objStr.contains("\"route\":4"))
    }

    @Test
    fun orderNumberSerializerDecodesBothShapes() {
        val json = Json
        val plain = json.decodeFromString(INumbering.OrderNumber.serializer(), """"2015550123"""")
        assertEquals(INumbering.OrderNumber.of("2015550123"), plain)
        val obj = json.decodeFromString(INumbering.OrderNumber.serializer(),
            """{"number":"2015550123","route":4}""")
        assertEquals(INumbering.OrderNumber.of("2015550123", 4), obj)
    }

    @Test
    fun orderNumberOfWithNullRouteUsesPlain() {
        val plain = INumbering.OrderNumber.of("2015550123", null)
        assertEquals(INumbering.OrderNumber.of("2015550123"), plain)
    }

    // -- Coverage: Account add KDoc convenience defaults --
    @Test
    fun accountAddDefaultMasterAccount() {
        val r = com.voicetel.sdk.models.Account.AddRequest(
            username = "1000000002", name = "x", email = "y")
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
        assertEquals("2.2.17", Version.SDK_VERSION)
        assertEquals("v2.2.17", Version.API_VERSION)
        assertTrue(Version.DEFAULT_USER_AGENT.contains("kotlin"))
        assertTrue(Version.DEFAULT_USER_AGENT.contains("2.2.17"))
    }

    // -- Coverage: ClientOptions defaults companion --
    @Test
    fun clientOptionsDefaultsCompanion() {
        val o = ClientOptions.defaults()
        assertEquals("", o.apiKey)
        assertEquals(2, o.maxRetries)
    }

    // -- Coverage: Authentication type constants match the spec mapping --
    // 0=Digest, 1=Lax (Digest | IP), 2=IP, 3=Strict (Digest + IP).
    @Test
    fun authenticationTypeConstants() {
        assertEquals(0, com.voicetel.sdk.models.Authentication.TYPE_DIGEST)
        assertEquals(1, com.voicetel.sdk.models.Authentication.TYPE_DIGEST_OR_IP)
        assertEquals(2, com.voicetel.sdk.models.Authentication.TYPE_IP_AUTH)
        assertEquals(3, com.voicetel.sdk.models.Authentication.TYPE_DIGEST_AND_IP)
    }

    // -- Coverage: fields removed in API 2.2.11 stay off the wire models --
    @Test
    fun removedFieldsNotSerialized() {
        val json = Json
        val services = json.encodeToString(
            Account.Services.serializer(), Account.Services(sms = true))
        assertTrue("\"rcid\"" !in services)
        val port = json.encodeToString(
            INumbering.PortSummary.serializer(), INumbering.PortSummary(status = "Complete"))
        assertTrue("\"supportUrl\"" !in port)
    }

    // -- Coverage: campaign-assign orderId accepts string or number --
    @Test
    fun campaignAssignOrderIdDecodesBothTypes() {
        val json = Json
        val str = json.decodeFromString(Numbers.MessagingCampaignAssignData.serializer(),
            """{"orderId":"ORD-1"}""")
        assertEquals(JsonPrimitive("ORD-1"), str.orderId)
        val num = json.decodeFromString(Numbers.MessagingCampaignAssignData.serializer(),
            """{"orderId":12345}""")
        assertEquals(JsonPrimitive(12345), num.orderId)
    }

    // -- Coverage: messaging state decodes network C plus a pending job --
    @Test
    fun messagingStateDecodesPendingJob() {
        val state = Json.decodeFromString(Numbers.MessagingState.serializer(),
            """{"network":"C","pending":{"campaignId":"CAMP002","state":"routing",""" +
                """"message":"assigning routes","createdAt":"2026-01-01T00:00:00Z",""" +
                """"updatedAt":"2026-01-01T00:01:00Z"}}""")
        assertEquals("C", state.network)
        assertEquals("CAMP002", state.pending?.campaignId)
        assertEquals("routing", state.pending?.state)
    }
}
