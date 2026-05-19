package com.voicetel.sdk

import com.voicetel.sdk.models.Account
import com.voicetel.sdk.resources.AccountService
import com.voicetel.sdk.resources.AclService
import com.voicetel.sdk.resources.AuthenticationService
import com.voicetel.sdk.resources.E911Service
import com.voicetel.sdk.resources.GatewaysService
import com.voicetel.sdk.resources.INumberingService
import com.voicetel.sdk.resources.LookupsService
import com.voicetel.sdk.resources.MessagingService
import com.voicetel.sdk.resources.NumbersService
import com.voicetel.sdk.resources.SupportService
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/**
 * Entry point for the VoiceTel API.
 *
 * Construct directly with a [ClientOptions], or with the DSL builder:
 * ```
 * val client = VoiceTelClient {
 *     apiKey = "32hex..."
 *     timeoutMillis = 30_000
 *     maxRetries = 2
 * }
 * ```
 *
 * All operations are `suspend` and cancellation-cooperative.
 *
 * ```
 * val client = VoiceTelClient()
 * client.login(1000000001, "hunter2")
 * val me: Account.Data = client.account.get()
 * ```
 */
public class VoiceTelClient(options: ClientOptions = ClientOptions.defaults()) : AutoCloseable {
    private val transport: Transport = Transport(options)

    public val account: AccountService = AccountService(transport)
    public val acl: AclService = AclService(transport)
    public val authentication: AuthenticationService = AuthenticationService(transport)
    public val e911: E911Service = E911Service(transport)
    public val gateways: GatewaysService = GatewaysService(transport)
    public val iNumbering: INumberingService = INumberingService(transport)
    public val lookups: LookupsService = LookupsService(transport)
    public val messaging: MessagingService = MessagingService(transport)
    public val numbers: NumbersService = NumbersService(transport)
    public val support: SupportService = SupportService(transport)

    /** Currently installed bearer token (empty string before [login]). */
    public val apiKey: String get() = transport.getApiKey()

    /** API base URL this client is configured against. */
    public val baseUrl: String get() = transport.getBaseUrl()

    /**
     * Exchange username + password for a 32-hex API key and install it on this client.
     *
     * This call counts against the 6 req/hour/IP rate limit shared by every
     * account endpoint (cdr, mrc, payments, registration, api-key).
     *
     * @return the new API key
     */
    public suspend fun login(username: Int, password: String): String {
        val body = Account.ApiKeyRequest(username, password)
        val data: Account.ApiKeyData = transport.request(
            method = HttpMethod.Post,
            path = "/v2.2/account/api-key",
            query = null,
            bodySerializer = @Suppress("UNCHECKED_CAST") (Account.ApiKeyRequest.serializer() as KSerializer<Any>),
            bodyValue = body,
            responseSerializer = Account.ApiKeyData.serializer(),
            requireAuth = false,
        )
        if (data.apikey.isEmpty()) {
            throw ApiError(
                "api-key response did not contain data.apikey",
                kind = ErrorKind.Authentication,
            )
        }
        transport.setBearer(data.apikey)
        return data.apikey
    }

    /** Release the underlying Ktor HttpClient (if owned). */
    override fun close() {
        transport.close()
    }
}

/** DSL builder for [VoiceTelClient]. */
public fun VoiceTelClient(configure: ClientOptions.() -> Unit): VoiceTelClient {
    val opts = ClientOptions().apply(configure)
    return VoiceTelClient(opts)
}
