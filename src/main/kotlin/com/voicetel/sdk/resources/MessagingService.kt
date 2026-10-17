package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Messaging
import com.voicetel.sdk.models.Numbers
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** SMS / MMS sending and 10DLC brand/campaign registration. */
public class MessagingService internal constructor(private val t: Transport) {

    /**
     * Filters for [history]. [number] (ten-digit TN on the account) is
     * required; [from]/[to] are Unix timestamps (defaults: now / 24h ago);
     * [type] is `sms` (default), `mms`, or `dlr`.
     */
    public class HistoryOptions(
        public val number: String,
        public val from: Int? = null,
        public val to: Int? = null,
        public val type: String? = null,
    ) {
        public companion object {
            public fun forNumber(n: String): HistoryOptions = HistoryOptions(number = n)
        }
    }

    public suspend fun history(opts: HistoryOptions): Messaging.HistoryData {
        val q = mutableMapOf<String, Any?>()
        q["number"] = opts.number
        opts.from?.let { q["from"] = it }
        opts.to?.let { q["to"] = it }
        opts.type?.let { q["type"] = it }
        return t.request(HttpMethod.Get, "/v2.2/messages", q, null, null,
            Messaging.HistoryData.serializer(), true)
    }

    public suspend fun send(body: Messaging.SendRequest): Messaging.SendData =
        t.request(HttpMethod.Post, "/v2.2/messages", null,
            @Suppress("UNCHECKED_CAST") (Messaging.SendRequest.serializer() as KSerializer<Any>), body,
            Messaging.SendData.serializer(), true)

    public suspend fun createBrand(body: Messaging.BrandCreateRequest): Messaging.BrandCreateData =
        t.request(HttpMethod.Post, "/v2.2/messaging/brands", null,
            @Suppress("UNCHECKED_CAST") (Messaging.BrandCreateRequest.serializer() as KSerializer<Any>), body,
            Messaging.BrandCreateData.serializer(), true)

    /** [ids] is a comma-separated list of campaign ids (max 100). */
    public suspend fun campaignStatus(ids: String): Messaging.CampaignStatusData {
        val q = mutableMapOf<String, Any?>()
        q["ids"] = ids
        return t.request(HttpMethod.Get, "/v2.2/messaging/campaigns", q, null, null,
            Messaging.CampaignStatusData.serializer(), true)
    }

    public suspend fun createCampaign(body: Messaging.CampaignCreateRequest): Messaging.CampaignCreateData =
        t.request(HttpMethod.Post, "/v2.2/messaging/campaigns", null,
            @Suppress("UNCHECKED_CAST") (Messaging.CampaignCreateRequest.serializer() as KSerializer<Any>), body,
            Messaging.CampaignCreateData.serializer(), true)

    /** Messaging state for many numbers at once (max 1000). */
    public suspend fun numbersState(numbers: List<String>): Numbers.MessagingListData {
        val q = mutableMapOf<String, Any?>()
        q["numbers"] = numbers.joinToString(",")
        return t.request(HttpMethod.Get, "/v2.2/numbers/messaging", q, null, null,
            Numbers.MessagingListData.serializer(), true)
    }

    /**
     * Account-wide messaging health overview — the same view as the portal Messaging tab:
     * summary counts (numbers, messaging on, healthy, needing attention, in progress),
     * campaigns with carrier-review progress, numbers needing attention (each with a
     * plain-language title, detail and the actions that fix it), 14 days of delivery
     * counts, and the alert-email setting. Updated hourly.
     *
     * The API spec defines no response schema for this endpoint, so the decoded JSON
     * object is returned as a raw [JsonObject] rather than a typed model.
     */
    public suspend fun health(): JsonObject =
        t.request(HttpMethod.Get, "/v2.2/messaging/health", null, null, null,
            JsonObject.serializer(), true)

    /**
     * The alert-email setting: whether new messaging problems are emailed to the account.
     *
     * The spec defines no response schema, so the decoded JSON object is returned raw.
     */
    public suspend fun healthAlerts(): JsonObject =
        t.request(HttpMethod.Get, "/v2.2/messaging/health/alerts", null, null, null,
            JsonObject.serializer(), true)

    /**
     * Turn alert emails on or off. When enabled, the account is emailed as new messaging
     * problems are found (at most one digest about every 20 hours).
     *
     * The spec defines no response schema, so the decoded JSON object is returned raw.
     */
    public suspend fun setHealthAlerts(enabled: Boolean): JsonObject =
        t.request(HttpMethod.Put, "/v2.2/messaging/health/alerts", null,
            @Suppress("UNCHECKED_CAST") (JsonObject.serializer() as KSerializer<Any>),
            JsonObject(mapOf("enabled" to JsonPrimitive(enabled))),
            JsonObject.serializer(), true)

    /**
     * Live health check of one number: in service, messaging enabled, campaign approved,
     * assigned to the campaign by the messaging provider, delivery failures in the last 24
     * hours, and enablement in progress. Limited to 30 checks per hour per account.
     *
     * The spec defines no response schema, so the decoded JSON object is returned raw.
     */
    public suspend fun healthCheck(number: String): JsonObject =
        t.request(HttpMethod.Post, "/v2.2/messaging/health/check", null,
            @Suppress("UNCHECKED_CAST") (JsonObject.serializer() as KSerializer<Any>),
            JsonObject(mapOf("number" to JsonPrimitive(number))),
            JsonObject.serializer(), true)

    /**
     * Up to 3 recent outbound messages from the number that carriers blocked as spam
     * (time, masked recipient, text, reason), to help review content.
     *
     * The spec defines no response schema, so the decoded JSON object is returned raw.
     */
    public suspend fun healthSamples(number: String): JsonObject {
        val q = mutableMapOf<String, Any?>("number" to number)
        return t.request(HttpMethod.Get, "/v2.2/messaging/health/samples", q, null, null,
            JsonObject.serializer(), true)
    }
}
