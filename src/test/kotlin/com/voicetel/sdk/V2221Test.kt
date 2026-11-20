package com.voicetel.sdk

import com.voicetel.sdk.models.Fax
import com.voicetel.sdk.resources.FaxService
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-shape tests for the v2.2.21 additions, asserted against the v2.2 spec.
 *
 * Covers:
 * - GET /v2.2/numbers/fax (fax-capable numbers)
 * - POST /v2.2/faxes (multipart body, UUID Idempotency-Key, 201, retry reuses the key)
 * - GET /v2.2/faxes (history query params: number/from/to/limit/cursor/status)
 * - GET /v2.2/faxes/{id} (full job shape)
 * - GET /v2.2/faxes/{id}/document (binary download)
 * - the fax error envelope's extra `serverTime` field is tolerated
 * - POST /v2.2/faxes 402/413 map to PaymentRequired/RequestTooLarge error kinds
 * - DELETE /v2.2/acl sends the {"acl":[{cidr}]} body with no cidrs query
 */
class V2221Test {

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** Drain a sent multipart body to bytes so its wire shape can be asserted. */
    private suspend fun multipartBody(content: MultiPartFormDataContent): String {
        val channel = ByteChannel()
        content.writeTo(channel)
        channel.flushAndClose()
        return channel.toByteArray().decodeToString()
    }

    /** Render recorded query parameters as `a=1&b=2` (order preserved). */
    private fun queryString(params: io.ktor.http.Parameters): String =
        params.entries().joinToString("&") { (k, values) ->
            values.joinToString("&") { "$k=$it" }
        }

    // ------------------------------------------------------- fax numbers ---

    @Test
    fun faxNumbersListDecodesEntries() = runTest {
        val client = mockClient { req ->
            assertEquals(HttpMethod.Get, req.method)
            assertEquals("/v2.2/numbers/fax", req.url.encodedPath)
            respond(envelope("""{"numbers":[""" +
                """{"number":"2015550123","destinationId":7,"email":"fax@example.com"},""" +
                """{"number":"2125550123","destinationId":0,"email":"account@example.com"}]}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.fax.listNumbers()
        assertEquals(2, r.numbers.size)
        assertEquals("2015550123", r.numbers[0].number)
        assertEquals(7, r.numbers[0].destinationId)
        assertEquals("fax@example.com", r.numbers[0].email)
        assertEquals(0, r.numbers[1].destinationId, "destinationId 0 = account email")
        client.close()
    }

    // --------------------------------------------------------- fax send ---

    @Test
    fun faxSendPostsMultipartWithUuidIdempotencyKey() = runTest {
        val pdf = "%PDF-1.7 mock fax document".encodeToByteArray()
        val client = mockClient { req ->
            assertEquals(HttpMethod.Post, req.method)
            assertEquals("/v2.2/faxes", req.url.encodedPath)

            // With setBody(OutgoingContent) the media type travels on the body
            // (merged into the real request's Content-Type on the wire).
            val contentType = req.body.contentType?.toString()
                ?: req.headers[HttpHeaders.ContentType]
            assertNotNull(contentType)
            assertTrue(contentType.startsWith("multipart/form-data; boundary="),
                "content-type must be multipart, was: $contentType")

            val key = req.headers["Idempotency-Key"]
            assertNotNull(key, "Idempotency-Key is required on fax send")
            assertTrue(uuidRegex.matches(key), "Idempotency-Key must be a UUID, was: $key")

            val form = req.body as MultiPartFormDataContent
            val body = multipartBody(form)
            val boundary = form.boundary
            assertEquals(5, body.split("--$boundary").size,
                "boundary delimits two fields, one file part, and the terminator; body was:\n$body")
            assertTrue("name=\"fromNumber\"" in body, "body was:\n$body")
            assertTrue("2015550123" in body, "body was:\n$body")
            assertTrue("name=\"toNumber\"" in body, "body was:\n$body")
            assertTrue("2125550123" in body, "body was:\n$body")
            assertTrue("name=\"documents\"" in body, "body was:\n$body")
            assertTrue("filename=\"contract.pdf\"" in body, "body was:\n$body")
            assertTrue("Content-Type: application/pdf" in body, "body was:\n$body")
            assertTrue("%PDF-1.7 mock fax document" in body, "body was:\n$body")

            respond(envelope("""{"id":"9f8c2b41a","fromNumber":"2015550123",""" +
                """"toNumber":"2125550123","status":"queued",""" +
                """"documents":[{"name":"contract.pdf","size":26,"contentType":"application/pdf"}]}"""),
                HttpStatusCode.Created, jsonHeaders)
        }
        val r = client.fax.send(
            "2015550123", "2125550123",
            listOf(Fax.Document("contract.pdf", "application/pdf", pdf)),
        )
        assertEquals("9f8c2b41a", r.id)
        assertEquals("queued", r.status)
        assertEquals("contract.pdf", r.documents[0].name)
        assertEquals(26L, r.documents[0].size)
        client.close()
    }

    @Test
    fun faxSendUsesCallerSuppliedIdempotencyKey() = runTest {
        val client = mockClient { req ->
            assertEquals("123e4567-e89b-12d3-a456-426614174000", req.headers["Idempotency-Key"])
            respond(envelope("""{"id":"9f8c2b41a","status":"queued","documents":[]}"""),
                HttpStatusCode.Created, jsonHeaders)
        }
        val r = client.fax.send(
            "2015550123", "2125550123",
            listOf(Fax.Document("contract.pdf", "application/pdf", byteArrayOf('x'.code.toByte()))),
            idempotencyKey = "123e4567-e89b-12d3-a456-426614174000",
        )
        assertEquals("9f8c2b41a", r.id)
        client.close()
    }

    @Test
    fun faxSendRetryReusesTheSameIdempotencyKey() = runTest {
        // The key pins the submission across retries, so a retried send can
        // never deliver the same fax twice.
        var calls = 0
        val keys = mutableListOf<String>()
        val client = mockClientRetries(maxRetries = 1) { req ->
            keys += req.headers["Idempotency-Key"]!!
            if (++calls == 1) {
                respond("""{"status":"error","error":{"code":502,"message":"processor"}}""",
                    HttpStatusCode.BadGateway, jsonHeaders)
            } else {
                respond(envelope("""{"id":"9f8c2b41a","status":"queued","documents":[]}"""),
                    HttpStatusCode.Created, jsonHeaders)
            }
        }
        val r = client.fax.send(
            "2015550123", "2125550123",
            listOf(Fax.Document("contract.pdf", "application/pdf", byteArrayOf('x'.code.toByte()))),
        )
        assertEquals("9f8c2b41a", r.id)
        assertEquals(2, calls)
        assertEquals(keys[0], keys[1], "retries must reuse the Idempotency-Key")
        client.close()
    }

    // ------------------------------------------------------ fax history ---

    @Test
    fun faxHistorySendsQueryParameters() = runTest {
        val client = mockClient { req ->
            assertEquals(HttpMethod.Get, req.method)
            assertEquals("/v2.2/faxes", req.url.encodedPath)
            assertEquals(
                "number=2015550123&from=1795100000&to=1795013600&limit=50&cursor=cur1&status=failed",
                queryString(req.url.parameters),
            )
            respond(envelope("""{"number":"2015550123","fromTs":1795100000,"toTs":1795013600,""" +
                """"faxes":[],"nextCursor":null}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.fax.history(
            FaxService.HistoryOptions(
                number = "2015550123",
                from = 1795100000L,
                to = 1795013600L,
                limit = 50,
                cursor = "cur1",
                status = "failed",
            ),
        )
        assertEquals("2015550123", r.number)
        assertEquals(1795100000L, r.fromTs)
        assertNull(r.nextCursor)
        client.close()
    }

    @Test
    fun faxHistoryForNumberSendsOnlyTheRequiredParameter() = runTest {
        val client = mockClient { req ->
            assertEquals("number=2015550123", queryString(req.url.parameters))
            respond(envelope("""{"number":"2015550123","faxes":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.fax.history("2015550123")
        client.close()
    }

    // --------------------------------------------------------- fax get ---

    @Test
    fun faxGetDecodesFullJob() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/faxes/9f8c2b41a", req.url.encodedPath)
            respond(envelope("""{"id":"9f8c2b41a","fromNumber":"2015550123",""" +
                """"toNumber":"2125550123","clientToken":"123e4567-e89b-12d3-a456-426614174000",""" +
                """"status":"sent","submittedAt":"2026-11-20T15:04:05Z",""" +
                """"startedAt":"2026-11-20T15:04:41Z","completedAt":"2026-11-20T15:05:19Z",""" +
                """"attempts":1,"failureReason":null,""" +
                """"documents":[{"name":"contract.pdf","size":123456,"contentType":"application/pdf"}],""" +
                """"documentAvailable":true,"documentName":"contract.pdf","documentSize":123456}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val job = client.fax.get("9f8c2b41a")
        assertEquals("9f8c2b41a", job.id)
        assertEquals("2015550123", job.fromNumber)
        assertEquals("2125550123", job.toNumber)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", job.clientToken)
        assertEquals("sent", job.status)
        assertEquals("2026-11-20T15:04:05Z", job.submittedAt)
        assertEquals("2026-11-20T15:05:19Z", job.completedAt)
        assertEquals(1, job.attempts)
        assertNull(job.failureReason)
        assertEquals(1, job.documents.size)
        assertEquals("contract.pdf", job.documents[0].name)
        assertEquals(true, job.documentAvailable)
        assertEquals(123456L, job.documentSize)
        client.close()
    }

    // ---------------------------------------------------- fax document ---

    @Test
    fun faxDocumentReturnsRawPdfBytes() = runTest {
        // A payload with every byte value proves the document download hands
        // back raw bytes, never a mangled string.
        val pdf = ByteArray(256) { i -> i.toByte() }
        val client = mockClient { req ->
            assertEquals("/v2.2/faxes/9f8c2b41a/document", req.url.encodedPath)
            respond(pdf, HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.ContentType to listOf("application/pdf"),
                    HttpHeaders.ContentDisposition to listOf("attachment; filename=\"contract.pdf\""),
                ),
            )
        }
        val got = client.fax.document("9f8c2b41a")
        assertContentEquals(pdf, got)
        client.close()
    }

    // -------------------------------------------------- fax error shape ---

    @Test
    fun faxErrorEnvelopeWithServerTimeMapsToTypedError() = runTest {
        // The fax endpoints add "serverTime" beside status/error; error
        // decoding must tolerate it and still pull code/message.
        val client = mockClient { _ ->
            respond("""{"status":"error","error":{"code":404,"message":"Not found"},""" +
                """"serverTime":1795100000}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.fax.get("missing001") }
        assertTrue(err.isNotFound)
        assertEquals("Not found", err.message)
        assertEquals(404L, err.code)
        client.close()
    }

    @Test
    fun faxDocument404RaisesTypedApiError() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/faxes/none000001/document", req.url.encodedPath)
            respond("""{"status":"error","error":{"code":404,"message":"Not found"},""" +
                """"serverTime":1795100000}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.fax.document("none000001") }
        assertTrue(err.isNotFound)
        assertEquals("Not found", err.message)
        client.close()
    }

    @Test
    fun faxSendMaps402And413ToTypedErrorKinds() = runTest {
        // v2.2.21: 402 — the balance must be positive to send a fax;
        // 413 — the documents must total 8 MiB or smaller.
        val pdf = byteArrayOf('x'.code.toByte())

        val poor = mockClient { _ ->
            respond("""{"status":"error","error":{"code":402,""" +
                """"message":"Your account needs a positive balance to send faxes."},""" +
                """"serverTime":1795100000}""", HttpStatusCode.PaymentRequired, jsonHeaders)
        }
        val e402 = assertFailsWith<ApiError> {
            poor.fax.send("2015550123", "2125550123",
                listOf(Fax.Document("contract.pdf", "application/pdf", pdf)))
        }
        assertEquals(ErrorKind.PaymentRequired, e402.kind)
        assertEquals(402, e402.statusCode)
        assertEquals(402L, e402.code)
        poor.close()

        val huge = mockClient { _ ->
            respond("""{"status":"error","error":{"code":413,""" +
                """"message":"The documents must total 8 MiB or smaller"},""" +
                """"serverTime":1795100000}""", HttpStatusCode.PayloadTooLarge, jsonHeaders)
        }
        val e413 = assertFailsWith<ApiError> {
            huge.fax.send("2015550123", "2125550123",
                listOf(Fax.Document("contract.pdf", "application/pdf", pdf)))
        }
        assertEquals(ErrorKind.RequestTooLarge, e413.kind)
        assertEquals(413, e413.statusCode)
        assertEquals(413L, e413.code)
        huge.close()
    }

    // ------------------------------------------------------- acl delete ---

    @Test
    fun aclRemoveSendsAclBodyWithoutCidrsQuery() = runTest {
        val client = mockClient { req ->
            assertEquals(HttpMethod.Delete, req.method)
            assertEquals("/v2.2/acl", req.url.encodedPath)
            assertTrue(req.url.parameters.names().isEmpty(),
                "query must be empty in v2.2.21, was: ${req.url.parameters}")
            val sent = (req.body as TextContent).text
            assertEquals("""{"acl":[{"cidr":"203.0.113.0/24"},{"cidr":"198.51.100.7/32"}]}""", sent)
            respond(envelope("""{"removed":[{"cidr":"203.0.113.0/24"}]}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val r = client.acl.remove(listOf("203.0.113.0/24", "198.51.100.7/32"))
        assertEquals(1, r.removed.size)
        assertEquals("203.0.113.0/24", r.removed[0].cidr)
        client.close()
    }

    @Test
    fun aclRemoveConflictCarriesRemovedAndFailedData() = runTest {
        val client = mockClient { _ ->
            respond(
                """{"status":"error","error":{"code":409,""" +
                    """"message":"One or more ACL entries could not be removed"},""" +
                    """"data":{"removed":[{"cidr":"203.0.113.0/24"}],""" +
                    """"failed":[{"cidr":"203.0.113.9/32","reason":"DB delete failed"}]}}""",
                HttpStatusCode.Conflict, jsonHeaders,
            )
        }
        val err = assertFailsWith<ApiError> {
            client.acl.remove(listOf("203.0.113.0/24", "203.0.113.9/32"))
        }
        assertTrue(err.isConflict)
        assertEquals(409L, err.code)
        val body = err.body
        assertNotNull(body)
        val root = Json.parseToJsonElement(body.toString()).jsonObject
        assertEquals(
            "DB delete failed",
            root["data"]!!.jsonObject["failed"]!!.jsonArray[0].jsonObject["reason"]!!.jsonPrimitive.content,
        )
        client.close()
    }
}
