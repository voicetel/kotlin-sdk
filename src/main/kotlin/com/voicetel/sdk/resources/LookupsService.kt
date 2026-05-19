package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Lookups
import io.ktor.http.HttpMethod

/** CNAM and LRN dips. Each call costs money. */
public class LookupsService internal constructor(private val t: Transport) {

    public suspend fun cnam(number: String): Lookups.CnamData =
        t.request(HttpMethod.Get, "/v2.2/cnam/$number", null, null, null,
            Lookups.CnamData.serializer(), true)

    /** `ani` is the presented caller ANI (10-digit TN), used only for billing/auth. */
    public suspend fun lrn(number: String, ani: String): Lookups.LrnLookupData =
        t.request(HttpMethod.Get, "/v2.2/lrn/$number/$ani", null, null, null,
            Lookups.LrnLookupData.serializer(), true)
}
