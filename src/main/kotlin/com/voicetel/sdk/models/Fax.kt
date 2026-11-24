package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** Fax-resource models (Fax API, spec v2.2.21). */
public object Fax {

    /** One fax-capable number on the account. [destinationId] 0 delivers to the account email. */
    @Serializable
    public data class NumberEntry(
        public val number: String? = null,
        public val destinationId: Int? = null,
        public val email: String? = null,
    )

    /** Payload of GET /v2.2/numbers/fax — every fax-capable number, ascending. */
    @Serializable
    public data class NumbersData(
        public val numbers: List<NumberEntry> = emptyList(),
    )

    /** Metadata of one document attached to a fax. */
    @Serializable
    public data class DocumentMeta(
        public val name: String? = null,
        public val size: Long? = null,
        public val contentType: String? = null,
    )

    /**
     * A document to send, as the caller supplies it (PDF/DOC/DOCX bytes,
     * magic-byte checked by the API — the file name's extension is not).
     */
    public data class Document(
        public val name: String,
        public val contentType: String,
        public val data: ByteArray,
    )

    /** Payload of POST /v2.2/faxes (HTTP 201) — the accepted job. */
    @Serializable
    public data class SendData(
        public val id: String? = null,
        public val fromNumber: String? = null,
        public val toNumber: String? = null,
        public val status: String? = null,
        public val documents: List<DocumentMeta> = emptyList(),
    )

    /**
     * One fax job, the same shape on every read (send echo, history, get).
     * Timestamps are ISO-8601 UTC strings; [attempts], [failureReason] and
     * [documentSize] are null while unknown.
     */
    @Serializable
    public data class Job(
        public val id: String? = null,
        public val fromNumber: String? = null,
        public val toNumber: String? = null,
        /** The Idempotency-Key the fax was submitted with, or null. */
        public val clientToken: String? = null,
        /** queued, sending, retry_pending, sent or failed. */
        public val status: String? = null,
        public val submittedAt: String? = null,
        public val startedAt: String? = null,
        public val completedAt: String? = null,
        public val attempts: Int? = null,
        public val failureReason: String? = null,
        public val documents: List<DocumentMeta> = emptyList(),
        public val documentAvailable: Boolean? = null,
        public val documentName: String? = null,
        public val documentSize: Long? = null,
    )

    /** One page of GET /v2.2/faxes — jobs newest first, plus the paging cursor. */
    @Serializable
    public data class HistoryData(
        public val number: String? = null,
        public val fromTs: Long? = null,
        public val toTs: Long? = null,
        public val faxes: List<Job> = emptyList(),
        public val nextCursor: String? = null,
    )
}
