package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.E911
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/**
 * e911 records and address validation.
 *
 * Requests take a 10-digit `dn`; responses return the 11-digit E.164 US form.
 */
public class E911Service internal constructor(private val t: Transport) {

    public suspend fun list(): E911.AllData =
        t.request(HttpMethod.Get, "/v2.2/e911", null, null, null, E911.AllData.serializer(), true)

    public suspend fun create(body: E911.CreateRequest): E911.RecordData =
        t.request(HttpMethod.Post, "/v2.2/e911", null,
            @Suppress("UNCHECKED_CAST") (E911.CreateRequest.serializer() as KSerializer<Any>), body,
            E911.RecordData.serializer(), true)

    public suspend fun validate(body: E911.AddressRequest): E911.ValidateData =
        t.request(HttpMethod.Post, "/v2.2/e911/validations", null,
            @Suppress("UNCHECKED_CAST") (E911.AddressRequest.serializer() as KSerializer<Any>), body,
            E911.ValidateData.serializer(), true)

    public suspend fun get(dn: String): E911.RecordData =
        t.request(HttpMethod.Get, "/v2.2/e911/$dn", null, null, null,
            E911.RecordData.serializer(), true)

    public suspend fun provision(dn: String, body: E911.ProvisionByIdRequest): E911.RecordData =
        t.request(HttpMethod.Put, "/v2.2/e911/$dn", null,
            @Suppress("UNCHECKED_CAST") (E911.ProvisionByIdRequest.serializer() as KSerializer<Any>), body,
            E911.RecordData.serializer(), true)

    /** Returns Unit on 204 No Content. */
    public suspend fun remove(dn: String) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/e911/$dn", null, null, true)
    }
}
