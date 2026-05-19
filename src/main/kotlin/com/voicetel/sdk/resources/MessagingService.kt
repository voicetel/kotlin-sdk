package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Messaging
import com.voicetel.sdk.models.Numbers
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** SMS / MMS sending and 10DLC brand/campaign registration. */
public class MessagingService internal constructor(private val t: Transport) {

    /** Filters for [history]. */
    public class HistoryOptions(
        public val number: String? = null,
        public val start: Int? = null,
        public val end: Int? = null,
        public val type: String? = null,
    ) {
        public companion object {
            public fun empty(): HistoryOptions = HistoryOptions()
            public fun forNumber(n: String): HistoryOptions = HistoryOptions(number = n)
        }
    }

    public suspend fun history(opts: HistoryOptions = HistoryOptions.empty()): Messaging.HistoryData {
        val q = mutableMapOf<String, Any?>()
        opts.number?.let { q["number"] = it }
        opts.start?.let { q["start"] = it }
        opts.end?.let { q["end"] = it }
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

    public suspend fun campaignStatus(): Messaging.CampaignStatusData =
        t.request(HttpMethod.Get, "/v2.2/messaging/campaigns", null, null, null,
            Messaging.CampaignStatusData.serializer(), true)

    public suspend fun createCampaign(body: Messaging.CampaignCreateRequest): Messaging.CampaignCreateData =
        t.request(HttpMethod.Post, "/v2.2/messaging/campaigns", null,
            @Suppress("UNCHECKED_CAST") (Messaging.CampaignCreateRequest.serializer() as KSerializer<Any>), body,
            Messaging.CampaignCreateData.serializer(), true)

    /** Messaging state for many numbers at once. Pass null for "all numbers". */
    public suspend fun numbersState(numbers: List<String>? = null): Numbers.MessagingListData {
        val q = mutableMapOf<String, Any?>()
        if (!numbers.isNullOrEmpty()) q["numbers"] = numbers.joinToString(",")
        return t.request(HttpMethod.Get, "/v2.2/numbers/messaging", q, null, null,
            Numbers.MessagingListData.serializer(), true)
    }
}
