package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Numbers
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.KSerializer

/** Every operation on a telephone number owned by the account. */
public class NumbersService internal constructor(private val t: Transport) {

    public suspend fun list(): Numbers.ListData =
        t.request(HttpMethod.Get, "/v2.2/numbers", null, null, null,
            Numbers.ListData.serializer(), true)

    public suspend fun add(body: Numbers.AddRequest): Numbers.AddData =
        t.request(HttpMethod.Post, "/v2.2/numbers", null,
            @Suppress("UNCHECKED_CAST") (Numbers.AddRequest.serializer() as KSerializer<Any>), body,
            Numbers.AddData.serializer(), true)

    public suspend fun get(number: String): Numbers.Detail =
        t.request(HttpMethod.Get, "/v2.2/numbers/${number.encodeURLPathPart()}", null, null, null,
            Numbers.Detail.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun remove(number: String) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/numbers/${number.encodeURLPathPart()}", null, null, true)
    }

    public suspend fun move(number: String, body: Numbers.MoveRequest): Numbers.MoveData =
        t.request(HttpMethod.Patch, "/v2.2/numbers/${number.encodeURLPathPart()}", null,
            @Suppress("UNCHECKED_CAST") (Numbers.MoveRequest.serializer() as KSerializer<Any>), body,
            Numbers.MoveData.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun release(number: String) {
        t.requestNoBody(HttpMethod.Post, "/v2.2/numbers/${number.encodeURLPathPart()}/release", null, null, true)
    }

    public suspend fun setRoute(number: String, body: Numbers.RouteRequest): Numbers.RouteData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/route", null,
            @Suppress("UNCHECKED_CAST") (Numbers.RouteRequest.serializer() as KSerializer<Any>), body,
            Numbers.RouteData.serializer(), true)

    public suspend fun setTranslation(number: String, body: Numbers.TranslationRequest): Numbers.TranslationData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/translation", null,
            @Suppress("UNCHECKED_CAST") (Numbers.TranslationRequest.serializer() as KSerializer<Any>), body,
            Numbers.TranslationData.serializer(), true)

    public suspend fun setCnam(number: String, body: Numbers.CnamRequest): Numbers.CnamData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/cnam", null,
            @Suppress("UNCHECKED_CAST") (Numbers.CnamRequest.serializer() as KSerializer<Any>), body,
            Numbers.CnamData.serializer(), true)

    public suspend fun setLidb(number: String, body: Numbers.LidbRequest): Numbers.LidbData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/lidb", null,
            @Suppress("UNCHECKED_CAST") (Numbers.LidbRequest.serializer() as KSerializer<Any>), body,
            Numbers.LidbData.serializer(), true)

    public suspend fun getFax(number: String): Numbers.FaxData =
        t.request(HttpMethod.Get, "/v2.2/numbers/${number.encodeURLPathPart()}/fax", null, null, null,
            Numbers.FaxData.serializer(), true)

    public suspend fun setFax(number: String, body: Numbers.FaxRequest): Numbers.FaxData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/fax", null,
            @Suppress("UNCHECKED_CAST") (Numbers.FaxRequest.serializer() as KSerializer<Any>), body,
            Numbers.FaxData.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun removeFax(number: String) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/numbers/${number.encodeURLPathPart()}/fax", null, null, true)
    }

    public suspend fun setForward(number: String, body: Numbers.ForwardRequest): Numbers.ForwardData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/forward", null,
            @Suppress("UNCHECKED_CAST") (Numbers.ForwardRequest.serializer() as KSerializer<Any>), body,
            Numbers.ForwardData.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun removeForward(number: String) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/numbers/${number.encodeURLPathPart()}/forward", null, null, true)
    }

    public suspend fun getSms(number: String): Numbers.SmsData =
        t.request(HttpMethod.Get, "/v2.2/numbers/${number.encodeURLPathPart()}/sms", null, null, null,
            Numbers.SmsData.serializer(), true)

    public suspend fun setSms(number: String, body: Numbers.SmsRequest): Numbers.SmsData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/sms", null,
            @Suppress("UNCHECKED_CAST") (Numbers.SmsRequest.serializer() as KSerializer<Any>), body,
            Numbers.SmsData.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun removeSms(number: String) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/numbers/${number.encodeURLPathPart()}/sms", null, null, true)
    }

    public suspend fun getMessaging(number: String): Numbers.MessagingState =
        t.request(HttpMethod.Get, "/v2.2/numbers/${number.encodeURLPathPart()}/messaging", null, null, null,
            Numbers.MessagingState.serializer(), true)

    public suspend fun patchMessaging(number: String, body: Numbers.MessagingPatchRequest): Numbers.MessagingPatchData =
        t.request(HttpMethod.Patch, "/v2.2/numbers/${number.encodeURLPathPart()}/messaging", null,
            @Suppress("UNCHECKED_CAST") (Numbers.MessagingPatchRequest.serializer() as KSerializer<Any>), body,
            Numbers.MessagingPatchData.serializer(), true)

    /**
     * Binds a 10DLC campaign to a TN. Path-B campaigns are bound synchronously
     * (`status` "enabled"); path-C campaigns are provisioned asynchronously
     * (HTTP 202, `status` "pending" — poll [getMessaging] until the job's
     * state is done or failed). Idempotent: a number already bound reports
     * `status` "already".
     */
    public suspend fun assignCampaign(number: String, body: Numbers.CampaignAssignRequest): Numbers.MessagingCampaignAssignData =
        t.request(HttpMethod.Put, "/v2.2/numbers/${number.encodeURLPathPart()}/messaging-campaign", null,
            @Suppress("UNCHECKED_CAST") (Numbers.CampaignAssignRequest.serializer() as KSerializer<Any>), body,
            Numbers.MessagingCampaignAssignData.serializer(), true)

    /** Unbinds one number from a 10DLC campaign. [campaignId] is required. */
    public suspend fun unassignCampaign(number: String, campaignId: String): Numbers.MessagingCampaignUnassignData {
        val q = mutableMapOf<String, Any?>()
        q["campaignId"] = campaignId
        return t.request(HttpMethod.Delete, "/v2.2/numbers/${number.encodeURLPathPart()}/messaging-campaign", q, null, null,
            Numbers.MessagingCampaignUnassignData.serializer(), true)
    }

    /**
     * Unbinds up to 1000 numbers from a 10DLC campaign. Both query params are
     * required; no request body is sent.
     */
    public suspend fun bulkUnassignCampaign(numbers: List<String>, campaignId: String): Numbers.BulkCampaignUnassignData {
        val q = mutableMapOf<String, Any?>()
        q["numbers"] = numbers.joinToString(",")
        q["campaignId"] = campaignId
        return t.request(HttpMethod.Delete, "/v2.2/numbers/messaging-campaign", q, null, null,
            Numbers.BulkCampaignUnassignData.serializer(), true)
    }

    public suspend fun setPortOutPin(number: String, body: Numbers.PortOutPinUpdateRequest): Numbers.PortOutPinUpdateData =
        t.request(HttpMethod.Patch, "/v2.2/numbers/${number.encodeURLPathPart()}/port-out-pin", null,
            @Suppress("UNCHECKED_CAST") (Numbers.PortOutPinUpdateRequest.serializer() as KSerializer<Any>), body,
            Numbers.PortOutPinUpdateData.serializer(), true)
}
