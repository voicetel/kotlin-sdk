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

    /**
     * Remove entries from the ACL. [cidrs] is required and sent as a
     * comma-separated query parameter (e.g. `203.0.113.0/24`); no request
     * body is sent.
     */
    public suspend fun remove(cidrs: List<String>): Acl.RemoveData {
        val q = mutableMapOf<String, Any?>()
        q["cidrs"] = cidrs.joinToString(",")
        return t.request(HttpMethod.Delete, "/v2.2/acl", q, null, null,
            Acl.RemoveData.serializer(), true)
    }
}
