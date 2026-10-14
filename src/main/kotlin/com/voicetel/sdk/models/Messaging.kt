package com.voicetel.sdk.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Messaging-resource models. */
public object Messaging {

    /**
     * Body for POST /v2.2/messages. Wire field names are `fromNumber` /
     * `toNumber`.
     */
    @Serializable
    public data class SendRequest(
        public val fromNumber: String,
        public val toNumber: String,
        public val text: String,
        public val subject: String? = null,
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

    @Serializable
    public data class RecordValue(
        public val sourceNumber: String? = null,
        public val destinationNumber: String? = null,
        public val direction: String? = null,
        public val rate: String? = null,
        public val number: Int? = null,
        public val message: String? = null,
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
