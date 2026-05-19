package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.Account
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/**
 * Operations under the Account tag.
 *
 * Note: [cdr], [recurringCharges], [payments], [registration] and
 * `client.login()` share a 6 req/hour/IP rate limit. Bursting will trigger 429s.
 */
public class AccountService internal constructor(private val t: Transport) {

    public suspend fun get(): Account.Data =
        t.request(HttpMethod.Get, "/v2.2/account", null, null, null, Account.Data.serializer(), true)

    public suspend fun update(body: Account.PutRequest): Account.PutData =
        t.request(HttpMethod.Put, "/v2.2/account", null,
            @Suppress("UNCHECKED_CAST") (Account.PutRequest.serializer() as KSerializer<Any>), body,
            Account.PutData.serializer(), true)

    public suspend fun add(body: Account.AddRequest): Account.AddData =
        t.request(HttpMethod.Post, "/v2.2/account", null,
            @Suppress("UNCHECKED_CAST") (Account.AddRequest.serializer() as KSerializer<Any>), body,
            Account.AddData.serializer(), true)

    /** Public sign-up flow. */
    public suspend fun signup(body: Account.SignupRequest): Account.SignupData =
        t.request(HttpMethod.Post, "/v2.2/accounts", null,
            @Suppress("UNCHECKED_CAST") (Account.SignupRequest.serializer() as KSerializer<Any>), body,
            Account.SignupData.serializer(), true)

    /** Rate-limited (6/hr/IP). */
    public suspend fun cdr(start: Int? = null, end: Int? = null): Account.CdrData {
        val q = mutableMapOf<String, Any?>()
        if (start != null) q["start"] = start
        if (end != null) q["end"] = end
        return t.request(HttpMethod.Get, "/v2.2/account/cdr", q, null, null, Account.CdrData.serializer(), true)
    }

    public suspend fun credits(): Account.CreditsData =
        t.request(HttpMethod.Get, "/v2.2/account/credits", null, null, null,
            Account.CreditsData.serializer(), true)

    /** Rate-limited (6/hr/IP). */
    public suspend fun recurringCharges(): Account.MrcData =
        t.request(HttpMethod.Get, "/v2.2/account/recurring-charges", null, null, null,
            Account.MrcData.serializer(), true)

    /** Rate-limited (6/hr/IP). */
    public suspend fun payments(): Account.PaymentsData =
        t.request(HttpMethod.Get, "/v2.2/account/payments", null, null, null,
            Account.PaymentsData.serializer(), true)

    /** Rate-limited (6/hr/IP). */
    public suspend fun registration(): Account.RegistrationData =
        t.request(HttpMethod.Get, "/v2.2/account/registration", null, null, null,
            Account.RegistrationData.serializer(), true)

    /** No auth required. */
    public suspend fun recover(body: Account.RecoverRequest): Account.RecoverData =
        t.request(HttpMethod.Post, "/v2.2/account/recovery", null,
            @Suppress("UNCHECKED_CAST") (Account.RecoverRequest.serializer() as KSerializer<Any>), body,
            Account.RecoverData.serializer(), false)
}
