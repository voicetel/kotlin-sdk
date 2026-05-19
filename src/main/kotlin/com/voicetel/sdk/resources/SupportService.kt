package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Support
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** Support tickets — create, read, update, delete, reply. */
public class SupportService internal constructor(private val t: Transport) {

    public suspend fun list(): Support.ListData =
        t.request(HttpMethod.Get, "/v2.2/support/tickets", null, null, null,
            Support.ListData.serializer(), true)

    public suspend fun create(body: Support.CreateRequest): Support.TicketData =
        t.request(HttpMethod.Post, "/v2.2/support/tickets", null,
            @Suppress("UNCHECKED_CAST") (Support.CreateRequest.serializer() as KSerializer<Any>), body,
            Support.TicketData.serializer(), true)

    public suspend fun get(id: Int): Support.TicketData =
        t.request(HttpMethod.Get, "/v2.2/support/tickets/$id", null, null, null,
            Support.TicketData.serializer(), true)

    public suspend fun update(id: Int, body: Support.UpdateRequest): Support.UpdateData =
        t.request(HttpMethod.Put, "/v2.2/support/tickets/$id", null,
            @Suppress("UNCHECKED_CAST") (Support.UpdateRequest.serializer() as KSerializer<Any>), body,
            Support.UpdateData.serializer(), true)

    /** Admin only. Returns Unit on 204 No Content. */
    public suspend fun delete(id: Int) {
        t.requestNoBody(HttpMethod.Delete, "/v2.2/support/tickets/$id", null, null, true)
    }

    public suspend fun messages(id: Int): Support.ThreadsData =
        t.request(HttpMethod.Get, "/v2.2/support/tickets/$id/messages", null, null, null,
            Support.ThreadsData.serializer(), true)

    public suspend fun reply(id: Int, body: Support.ReplyRequest): Support.ReplyData =
        t.request(HttpMethod.Post, "/v2.2/support/tickets/$id/replies", null,
            @Suppress("UNCHECKED_CAST") (Support.ReplyRequest.serializer() as KSerializer<Any>), body,
            Support.ReplyData.serializer(), true)
}
