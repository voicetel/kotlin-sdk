package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Lookups
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPathPart

/** CNAM, carrier/line-type, and LRN dips. Each call costs money. */
public class LookupsService internal constructor(private val t: Transport) {

    public suspend fun cnam(number: String): Lookups.CnamData =
        t.request(HttpMethod.Get, "/v2.2/cnam/${number.encodeURLPathPart()}", null, null, null,
            Lookups.CnamData.serializer(), true)

    /**
     * GET /v2.2/carrier/{number} — carrier name and line type (voip,
     * landline, mobile, ...). Billed per lookup at the account's CNAM rate,
     * only when a result is returned.
     */
    public suspend fun carrier(number: String): Lookups.CarrierData =
        t.request(HttpMethod.Get, "/v2.2/carrier/${number.encodeURLPathPart()}", null, null, null,
            Lookups.CarrierData.serializer(), true)

    /** `ani` is the presented caller ANI (10-digit TN), used only for billing/auth. */
    public suspend fun lrn(number: String, ani: String): Lookups.LrnLookupData =
        t.request(HttpMethod.Get, "/v2.2/lrn/${number.encodeURLPathPart()}/${ani.encodeURLPathPart()}", null, null, null,
            Lookups.LrnLookupData.serializer(), true)
}
