package com.voicetel.sdk.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Messaging-resource models. */
public object Messaging {

    /**
     * Body for POST /v2.2/messages. Wire field names are `fromNumber` /
     * `toNumber`.
     *
     * [text] is required for SMS and optional for MMS (at most 2048
     * characters there) — pass null with [mediaUrls] set to send a media-only
     * MMS. [mediaUrls], when set, must contain at least one publicly
     * reachable http(s) URL; its presence makes the message an MMS. Nulls are
     * not serialized — only the fields you set are sent.
     */
    @Serializable
    public data class SendRequest(
        public val fromNumber: String,
        public val toNumber: String,
        /** Message body (UTF-8). Required for SMS; optional for MMS (media-only MMS is valid). */
        public val text: String? = null,
        /** Deprecated by the API: accepted for compatibility but not delivered on most messaging routes; do not rely on it. */
        @Deprecated("subject is deprecated by the API and not delivered on most messaging routes")
        public val subject: String? = null,
        /** Publicly reachable http(s) URLs of the media to send; non-empty when set. Presence makes this an MMS. */
        public val mediaUrls: List<String>? = null,
    )

    @Serializable
    public data class BrandCreateRequest(
        public val messagingBrandId: String,
        public val messagingBrandName: String,
        public val messagingBrandDescription: String? = null,
    )

    /**
     * Body for POST /v2.2/messaging/campaigns, passed through to the campaign
     * registration service. [campaignClassName] (from the campaign registry)
     * and [campaignStartDate] (today) are auto-populated if omitted.
     */
    @Serializable
    public data class CampaignCreateRequest(
        public val messagingBrandId: String,
        public val externalCampaignId: String,
        public val campaignDescription: String,
        public val campaignClassName: String? = null,
        public val campaignStartDate: String? = null,
    )

    /**
     * Per-record fields inside [Record]. Shape varies by `type`.
     *
     * For `sms`/`mms`: [number] (the far-end TN), [direction], [rate] and
     * [message] populated — plus [ip] on outbound sms and [subject] /
     * [attachments] on mms (inbound: one object per media file; outbound: the
     * submitted URL(s) as a string or array). For `dlr`: [sourceNumber],
     * [destinationNumber], [messageText] (the receipt as received),
     * [transactionGUID] (the id of the message it receipts), and
     * [concatenatedMessage] on some multi-part receipts.
     */
    @Serializable
    public data class RecordValue(
        /** Your number that sent the original message (dlr). */
        public val sourceNumber: String? = null,
        /** Recipient of the original message (dlr). */
        public val destinationNumber: String? = null,
        /** in or out (sms, mms). */
        public val direction: String? = null,
        /** Rate charged for the message (sms, mms). */
        public val rate: String? = null,
        /** The other party: sender of an inbound message, recipient of an outbound one (sms, mms). */
        public val number: String? = null,
        /** Message text (sms, mms). May be empty for an MMS that carried only media. */
        public val message: String? = null,
        /** The delivery receipt as received, including its status (dlr). */
        public val messageText: String? = null,
        /** Id of the message this receipt is for (dlr). */
        public val transactionGUID: String? = null,
        /** Present on some receipts for multi-part messages (dlr). */
        public val concatenatedMessage: String? = null,
        /** mms only. Inbound: one object per media file; outbound: the submitted URL(s) as a string or array. */
        public val attachments: JsonElement? = null,
        /** Subject, if one was sent or received (mms). */
        public val subject: String? = null,
        /** Address the send request came from (outbound sms). */
        public val ip: String? = null,
    )

    @Serializable
    public data class Record(
        public val id: String? = null,
        public val key: List<JsonElement>? = null,
        public val value: RecordValue? = null,
    )

    @Serializable
    public data class HistoryData(
        public val number: String? = null,
        public val type: String? = null,
        public val fromTs: Int? = null,
        public val toTs: Int? = null,
        public val messages: List<Record> = emptyList(),
    )

    @Serializable
    public data class SendData(
        public val id: String? = null,
        public val type: String? = null,
        public val fromNumber: String? = null,
        public val toNumber: String? = null,
        public val parts: Int? = null,
        public val subject: String? = null,
        public val mediaUrls: List<String>? = null,
    )

    @Serializable
    public data class RegistrationResult(
        public val statusCode: String? = null,
        public val status: String? = null,
    )

    @Serializable
    public data class BrandCreateData(public val result: RegistrationResult? = null)

    @Serializable
    public data class CampaignCreateData(public val result: RegistrationResult? = null)

    /** One carrier's latest DCA decision, as received. */
    @Serializable
    public data class CampaignDcaCarrier(
        public val carrier: String? = null,
        /** ACTIVE, REJECTED, SUSPENDED, UNSUSPENDED or TERMINATED. */
        public val status: String? = null,
        /** The reviewer's note, as given. */
        public val detail: String? = null,
        public val updatedAt: String? = null,
    )

    /** Carrier (DCA) review progress for a campaign; null for CAMPAIGN_INVALID_ID. */
    @Serializable
    public data class CampaignDca(
        /** True once every carrier has a DCA assigned to review the campaign. */
        public val electionComplete: Boolean? = null,
        /** Carriers with a DCA assigned so far (null when unavailable). */
        public val electedCarriers: List<String>? = null,
        public val carriers: List<CampaignDcaCarrier> = emptyList(),
    )

    @Serializable
    public data class CampaignStatusItem(
        public val id: String? = null,
        /** e.g. CAMPAIGN_DCA_COMPLETE, CAMPAIGN_DCA_COMPLETE_NO_NUMBERS_ASSIGNED, CAMPAIGN_PENDING_DCA, CAMPAIGN_DCA_REJECTED, CAMPAIGN_INVALID_ID. */
        public val status: String? = null,
        public val numbers: List<String> = emptyList(),
        /** Carrier (DCA) review progress; null for CAMPAIGN_INVALID_ID. */
        public val dca: CampaignDca? = null,
    )

    @Serializable
    public data class CampaignStatusData(public val campaigns: List<CampaignStatusItem> = emptyList())
}
