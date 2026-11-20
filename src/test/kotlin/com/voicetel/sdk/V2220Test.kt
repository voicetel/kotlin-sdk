package com.voicetel.sdk

import com.voicetel.sdk.models.E911
import com.voicetel.sdk.models.INumbering
import com.voicetel.sdk.models.Messaging
import com.voicetel.sdk.models.Numbers
import com.voicetel.sdk.resources.MessagingService
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-shape tests for the v2.2.20 additions, asserted against the v2.2 spec.
 *
 * Covers:
 * - GET/PUT/DELETE /v2.2/numbers/{number}/directory-listing (202 on writes)
 * - GET /v2.2/carrier/{number}
 * - e911 addressid is a string on the wire
 * - POST /v2.2/orders 201 includes routes: [{number, route}]
 * - POST /v2.2/messages without text (media-only MMS)
 * - message-history value records carry the new v2.2.18–v2.2.20 fields
 */
class V2220Test {

    private val listing = """{"type":"B","name":"Acme Corp","streetNumber":"100",""" +
        """"streetName":"Broadway","streetType":"St","streetSuffix":"","location":"Suite 900",""" +
        """"city":"New York","state":"NY","zip":"10005","complex":false}"""

    private val echoedListing = """{"type":"B","name":"Acme Corp","streetNumber":"100",""" +
        """"streetName":"Broadway","streetType":"St","streetSuffix":"","location":"Suite 900",""" +
        """"city":"New York","state":"NY","zip":"10005"}"""

    // ------------------------------------------------------ directory listing ---

    @Test
    fun directoryListingGet() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/numbers/2015550123/directory-listing", req.url.encodedPath)
            assertEquals(HttpMethod.Get, req.method)
            respond(envelope("""{"number":"2015550123","status":"listed","eligible":true,""" +
                """"listing":$listing,"pending":null}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.numbers.getDirectoryListing("2015550123")
        assertEquals("listed", r.status)
        assertEquals(true, r.eligible)
        assertEquals("Acme Corp", r.listing?.name)
        assertEquals("Suite 900", r.listing?.location)
        assertEquals(false, r.listing?.complex)
        assertNull(r.pending)
        assertNull(r.orderId)
        client.close()
    }

    @Test
    fun directoryListingPutSendsBodyAndDecodes202() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/numbers/2015550123/directory-listing", req.url.encodedPath)
            assertEquals(HttpMethod.Put, req.method)
            val sent = (req.body as TextContent).text
            assertTrue("\"type\":\"business\"" in sent, "body was: $sent")
            assertTrue("\"city\":\"New York\"" in sent, "body was: $sent")
            assertTrue("\"zip\":\"10005\"" in sent, "body was: $sent")
            // Only fields the caller set are emitted.
            assertFalse("\"firstName\"" in sent, "body was: $sent")
            assertFalse("\"complex\"" in sent, "body was: $sent")
            respond(envelope("""{"number":"2015550123","status":"pending","orderId":"1026472",""" +
                """"listing":$echoedListing}"""), HttpStatusCode.Accepted, jsonHeaders)
        }
        val r = client.numbers.setDirectoryListing(
            "2015550123",
            Numbers.DirectoryListingRequest(
                type = "business",
                name = "Acme Corp",
                streetNumber = "100",
                streetName = "Broadway",
                streetType = "St",
                location = "Suite 900",
                city = "New York",
                state = "NY",
                zip = "10005",
            ),
        )
        // 202 is a success; the change is applied in the background.
        assertEquals("pending", r.status)
        assertEquals("1026472", r.orderId)
        assertEquals("Acme Corp", r.listing?.name)
        assertNull(r.listing?.complex, "the echoed listing carries no complex flag")
        client.close()
    }

    @Test
    fun directoryListingDeleteDecodes202() = runTest {
        val client = mockClient { req ->
            assertEquals(HttpMethod.Delete, req.method)
            respond(envelope("""{"number":"2015550123","status":"removing","orderId":"1026473"}"""),
                HttpStatusCode.Accepted, jsonHeaders)
        }
        val r = client.numbers.removeDirectoryListing("2015550123")
        assertEquals("removing", r.status)
        assertEquals("1026473", r.orderId)
        client.close()
    }

    @Test
    fun directoryListingCancelReturnsNotListed() = runTest {
        // Deleting while an add is still pending cancels it: no carrier order.
        val client = mockClient { _ ->
            respond(envelope("""{"number":"2015550123","status":"not_listed"}"""),
                HttpStatusCode.Accepted, jsonHeaders)
        }
        val r = client.numbers.removeDirectoryListing("2015550123")
        assertEquals("not_listed", r.status)
        assertNull(r.orderId)
        client.close()
    }

    // -------------------------------------------------------------- carrier ---

    @Test
    fun carrierLookupDecodesUpstreamSnakeCaseFields() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/carrier/2015550123", req.url.encodedPath)
            assertEquals(HttpMethod.Get, req.method)
            respond(envelope("""{"number":"2015550123","carrier":{"name":"EXAMPLE CARRIER",""" +
                """"type":"voip","mobile_country_code":null,"mobile_network_code":null,""" +
                """"error_code":null}}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.lookups.carrier("2015550123")
        assertEquals("2015550123", r.number)
        assertEquals("EXAMPLE CARRIER", r.carrier?.name)
        assertEquals("voip", r.carrier?.type)
        assertNull(r.carrier?.mobileCountryCode)
        assertNull(r.carrier?.mobileNetworkCode)
        assertNull(r.carrier?.errorCode)
        client.close()
    }

    @Test
    fun carrierLookupMobileFieldsDecode() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"number":"2015550100","carrier":{"name":"MOBILE CARRIER",""" +
                """"type":"mobile","mobile_country_code":"310","mobile_network_code":"456",""" +
                """"error_code":0}}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.lookups.carrier("2015550100")
        assertEquals("310", r.carrier?.mobileCountryCode)
        assertEquals("456", r.carrier?.mobileNetworkCode)
        assertEquals(0, r.carrier?.errorCode)
        client.close()
    }

    // ----------------------------------------------------------------- e911 ---

    @Test
    fun e911AddressidRoundTripsAsString() = runTest {
        // v2.2.20: the validated-address id is a hex string, not an integer,
        // on both the validation response and the provision-by-id request.
        val client = mockClient { req ->
            when {
                req.method == HttpMethod.Post && req.url.encodedPath == "/v2.2/e911/validations" ->
                    respond(envelope("""{"address":{"addressid":"123a45b678c9d012e3f45a67b890c123",""" +
                        """"address1":"100 Broadway","city":"New York","state":"NY","zip":"10005"}}"""),
                        HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Put && req.url.encodedPath == "/v2.2/e911/2015550123" -> {
                    val sent = (req.body as TextContent).text
                    assertTrue("\"addressid\":\"123a45b678c9d012e3f45a67b890c123\"" in sent,
                        "addressid must be sent as a string, body was: $sent")
                    respond(envelope("""{"record":{"dn":"12015550123","callername":"Acme"}}"""),
                        HttpStatusCode.OK, jsonHeaders)
                }
                else -> error("unexpected ${req.method} ${req.url.encodedPath}")
            }
        }
        val v = client.e911.validate(
            E911.AddressRequest("100 Broadway", null, "New York", "NY", "10005"))
        assertEquals("123a45b678c9d012e3f45a67b890c123", v.address.addressid)
        val p = client.e911.provision(
            "2015550123", E911.ProvisionByIdRequest("Acme", v.address.addressid!!))
        assertEquals("Acme", p.record.callername)
        client.close()
    }

    // ------------------------------------------------------------- ordering ---

    @Test
    fun ordersCreateDecodesRoutes() = runTest {
        // v2.2.20: POST /v2.2/orders returns the route each ordered number
        // was placed on.
        val client = mockClient { req ->
            assertEquals(HttpMethod.Post, req.method)
            assertEquals("/v2.2/orders", req.url.encodedPath)
            respond(envelope("""{"orderId":"ORD-1","amountCharged":1.0,""" +
                """"numbersOrdered":["2015550100"],"failed":[],""" +
                """"routes":[{"number":"2015550100","route":4}]}"""),
                HttpStatusCode.Created, jsonHeaders)
        }
        val r = client.iNumbering.order(
            INumbering.OrderCreateRequest(listOf(INumbering.OrderNumber.of("2015550100"))))
        assertEquals("ORD-1", r.orderId)
        assertEquals("2015550100", r.routes.single().number)
        assertEquals(4, r.routes.single().route)
        client.close()
    }

    // ------------------------------------------------------------- messaging ---

    @Test
    fun messageSendMediaOnlyMmsOmitsText() = runTest {
        // v2.2.20: text is optional (media-only MMS is valid); subject is
        // deprecated. Nulls are not serialized.
        val client = mockClient { req ->
            assertEquals(HttpMethod.Post, req.method)
            val sent = (req.body as TextContent).text
            assertFalse("\"text\"" in sent, "media-only MMS must omit text, body was: $sent")
            assertFalse("\"subject\"" in sent, "body was: $sent")
            assertTrue("\"mediaUrls\":[\"https://example.com/photo.jpg\"]" in sent,
                "body was: $sent")
            respond(envelope("""{"id":"m-1","type":"mms","fromNumber":"2015550123",""" +
                """"toNumber":"2015550100","parts":1}"""), HttpStatusCode.Accepted, jsonHeaders)
        }
        val r = client.messaging.send(
            Messaging.SendRequest("2015550123", "2015550100", mediaUrls = listOf("https://example.com/photo.jpg")))
        assertEquals("mms", r.type)
        assertEquals(1, r.parts)
        client.close()
    }

    @Test
    fun messageHistoryValueDecodesNewFields() = runTest {
        val client = mockClient { req ->
            assertEquals("2015550123", req.url.parameters["number"])
            respond(envelope("""{"number":"2015550123","type":"dlr","messages":[""" +
                """{"id":"r1","key":["12015550123",1747345100],""" +
                """"value":{"sourceNumber":"12015550123","destinationNumber":"12015550100",""" +
                """"messageText":"stat:DELIVRD","transactionGUID":"g-1",""" +
                """"concatenatedMessage":"2"}},""" +
                """{"id":"m2","key":[1,"2015550123",1747345200],""" +
                """"value":{"direction":"in","number":"2015550123","message":"",""" +
                """"attachments":[{"contentFile":"https://x/f.jpg","contentType":"image/jpeg"}],""" +
                """"subject":"Photo","ip":null}}]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.messaging.history(MessagingService.HistoryOptions.forNumber("2015550123"))

        val dlr = r.messages[0].value!!
        assertEquals("stat:DELIVRD", dlr.messageText)
        assertEquals("g-1", dlr.transactionGUID)
        assertEquals("2", dlr.concatenatedMessage)

        val mms = r.messages[1].value!!
        assertEquals("Photo", mms.subject)
        assertNull(mms.ip)
        val attachments = mms.attachments
        assertTrue(attachments is JsonArray, "inbound attachments decode as an array")
        assertEquals("https://x/f.jpg",
            attachments.jsonArray[0].jsonObject["contentFile"]?.jsonPrimitive?.content)
        client.close()
    }
}
