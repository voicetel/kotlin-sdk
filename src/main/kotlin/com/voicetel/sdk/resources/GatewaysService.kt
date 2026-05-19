package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Gateways
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** Outbound termination gateways. */
public class GatewaysService internal constructor(private val t: Transport) {

    public suspend fun list(): Gateways.ListData =
        t.request(HttpMethod.Get, "/v2.2/gateways", null, null, null,
            Gateways.ListData.serializer(), true)

    public suspend fun add(body: Gateways.AddRequest): Gateways.Entry =
        t.request(HttpMethod.Post, "/v2.2/gateways", null,
            @Suppress("UNCHECKED_CAST") (Gateways.AddRequest.serializer() as KSerializer<Any>), body,
            Gateways.Entry.serializer(), true)

    public suspend fun get(id: Int): Gateways.Entry =
        t.request(HttpMethod.Get, "/v2.2/gateways/$id", null, null, null,
            Gateways.Entry.serializer(), true)

    public suspend fun update(id: Int, body: Gateways.UpdateRequest): Gateways.Entry =
        t.request(HttpMethod.Put, "/v2.2/gateways/$id", null,
            @Suppress("UNCHECKED_CAST") (Gateways.UpdateRequest.serializer() as KSerializer<Any>), body,
            Gateways.Entry.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun remove(id: Int) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/gateways/$id", null, null, true)
    }

    public suspend fun numbers(id: Int): Gateways.NumbersData =
        t.request(HttpMethod.Get, "/v2.2/gateways/$id/numbers", null, null, null,
            Gateways.NumbersData.serializer(), true)
}
