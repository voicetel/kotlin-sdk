package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Authentication
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** SIP/HTTP authentication settings (mode + password). */
public class AuthenticationService internal constructor(private val t: Transport) {

    public suspend fun get(): Authentication.GetData =
        t.request(HttpMethod.Get, "/v2.2/auth", null, null, null,
            Authentication.GetData.serializer(), true)

    public suspend fun update(body: Authentication.PutRequest): Authentication.PutData =
        t.request(HttpMethod.Put, "/v2.2/auth", null,
            @Suppress("UNCHECKED_CAST") (Authentication.PutRequest.serializer() as KSerializer<Any>), body,
            Authentication.PutData.serializer(), true)
}
