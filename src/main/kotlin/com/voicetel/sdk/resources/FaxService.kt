package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Fax
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPathPart

/** Sending faxes from fax-capable numbers, history, and document downloads. */
public class FaxService internal constructor(private val t: Transport) {

    /** GET /v2.2/numbers/fax — every fax-capable number on the account, with the email each delivers to. */
    public suspend fun listNumbers(): Fax.NumbersData =
        t.request(HttpMethod.Get, "/v2.2/numbers/fax", null, null, null,
            Fax.NumbersData.serializer(), true)

    /**
     * POST /v2.2/faxes — send a fax as `multipart/form-data`.
     *
     * [documents] is 1–10 files of PDF/DOC/DOCX content (checked by magic
     * bytes, not extension), at most 8 MiB each and 8 MiB in total. A fresh
     * Idempotency-Key UUID is generated per call and reused across retries;
     * resubmitting the same documents with the same key returns the original
     * job instead of sending twice (see [send] with an explicit key). Requires
     * an enabled account with a positive balance; rate-limited to 10 sends
     * per minute per account — the transport honors `Retry-After` on the 429.
     */
    public suspend fun send(
        fromNumber: String,
        toNumber: String,
        documents: List<Fax.Document>,
    ): Fax.SendData = send(fromNumber, toNumber, documents, idempotencyKey = null)

    /** [send] with an explicit Idempotency-Key (null = generate a fresh UUID). */
    public suspend fun send(
        fromNumber: String,
        toNumber: String,
        documents: List<Fax.Document>,
        idempotencyKey: String?,
    ): Fax.SendData {
        val form = MultiPartFormDataContent(
            formData {
                append("fromNumber", fromNumber)
                append("toNumber", toNumber)
                for (doc in documents) {
                    append(
                        "documents",
                        doc.data,
                        Headers.build {
                            append(HttpHeaders.ContentType, doc.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${doc.name}\"")
                        },
                    )
                }
            },
        )
        return t.requestMultipart("/v2.2/faxes", form, Fax.SendData.serializer(), true, idempotencyKey)
    }

    /**
     * Filters for [history]. [number] (a fax number on the account) is
     * required; [from]/[to] are the newer/older range edges in unix seconds
     * (defaults: now / 24 hours ago); [limit] is 1–100 (default 50);
     * [cursor] continues from a previous nextCursor; [status] is one of
     * `all`, `active` (not yet finished), `queued`, `sending`,
     * `retry_pending`, `sent`, `failed`.
     */
    public class HistoryOptions(
        public val number: String,
        public val from: Long? = null,
        public val to: Long? = null,
        public val limit: Int? = null,
        public val cursor: String? = null,
        public val status: String? = null,
    ) {
        public companion object {
            public fun forNumber(n: String): HistoryOptions = HistoryOptions(number = n)
        }
    }

    /** GET /v2.2/faxes — the faxes sent from one number, newest first, cursor-paged. */
    public suspend fun history(opts: HistoryOptions): Fax.HistoryData {
        val q = mutableMapOf<String, Any?>()
        q["number"] = opts.number
        opts.from?.let { q["from"] = it }
        opts.to?.let { q["to"] = it }
        opts.limit?.let { q["limit"] = it }
        opts.cursor?.let { q["cursor"] = it }
        opts.status?.let { q["status"] = it }
        return t.request(HttpMethod.Get, "/v2.2/faxes", q, null, null,
            Fax.HistoryData.serializer(), true)
    }

    /** [history] with just the required [number]. */
    public suspend fun history(number: String): Fax.HistoryData =
        history(HistoryOptions.forNumber(number))

    /** GET /v2.2/faxes/{id} — one job; poll until [Fax.Job.status] is sent or failed. */
    public suspend fun get(id: String): Fax.Job =
        t.request(HttpMethod.Get, "/v2.2/faxes/${id.encodeURLPathPart()}", null, null, null,
            Fax.Job.serializer(), true)

    /** GET /v2.2/faxes/{id}/document — the rendered PDF as raw bytes (available when the job reports documentAvailable). */
    public suspend fun document(id: String): ByteArray =
        t.requestBytes(HttpMethod.Get, "/v2.2/faxes/${id.encodeURLPathPart()}/document", true)
}
