package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Acl
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** IP-based access control list. */
public class AclService internal constructor(private val t: Transport) {

    public suspend fun list(): Acl.ListData =
        t.request(HttpMethod.Get, "/v2.2/acl", null, null, null, Acl.ListData.serializer(), true)

    public suspend fun add(body: Acl.ModifyRequest): Acl.AddData =
        t.request(HttpMethod.Post, "/v2.2/acl", null,
            @Suppress("UNCHECKED_CAST") (Acl.ModifyRequest.serializer() as KSerializer<Any>), body,
            Acl.AddData.serializer(), true)

    public suspend fun remove(body: Acl.ModifyRequest): Acl.RemoveData =
        t.request(HttpMethod.Delete, "/v2.2/acl", null,
            @Suppress("UNCHECKED_CAST") (Acl.ModifyRequest.serializer() as KSerializer<Any>), body,
            Acl.RemoveData.serializer(), true)
}
