package com.voicetel.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Account-resource models — requests, response data shapes, and supporting entities. */
public object Account {

    @Serializable
    public data class Rates(
        public val cnam: Double? = null,
        public val intlMax: Double? = null,
        public val nibble: Double? = null,
        public val lrn: Double? = null,
        public val fax: Double? = null,
        public val tfAdj: Double? = null,
        public val did: Double? = null,
        public val mms: Double? = null,
        public val sms: Double? = null,
    )

    @Serializable
    public data class Services(
        public val e911: Boolean? = null,
        public val cnam: Boolean? = null,
        public val bypassMedia: Boolean? = null,
        public val intl: Boolean? = null,
        public val rcid: Boolean? = null,
        public val mms: Boolean? = null,
        public val dialer: Boolean? = null,
        public val sms: Boolean? = null,
    )

    /** Profile returned by GET /v2.2/account. */
    @Serializable
    public data class Data(
        public val username: String? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val enabled: Boolean? = null,
        public val created: String? = null,
        public val cash: Double? = null,
        public val callerId: String? = null,
        public val timezone: String? = null,
        public val authType: Int? = null,
        public val ccs: Int? = null,
        @SerialName("notify") public val notifyEnabled: Boolean? = null,
        public val notifyThreshold: Int? = null,
        public val rates: Rates? = null,
        public val services: Services? = null,
    )

    @Serializable
    public data class CreditEntry(
        public val date: String? = null,
        public val paid: Boolean? = null,
        public val amount: Double? = null,
    )

    @Serializable
    public data class PaymentEntry(
        public val transactionId: String? = null,
        public val date: String? = null,
        public val payerEmail: String? = null,
        public val status: String? = null,
        public val amount: Double? = null,
    )

    @Serializable
    public data class CdrEntryValue(
        public val dur: String? = null,
        public val dst: String? = null,
        public val ba: String? = null,
        public val nr: String? = null,
        public val cn: String? = null,
        public val ip: String? = null,
        public val cid: String? = null,
    )

    @Serializable
    public data class CdrEntry(
        public val id: String? = null,
        public val key: List<String>? = null,
        public val value: CdrEntryValue? = null,
    )

    @Serializable
    public data class CdrData(
        public val cdr: List<CdrEntry> = emptyList(),
        public val start: Int? = null,
        public val end: Int? = null,
    )

    @Serializable
    public data class CreditsData(public val credits: List<CreditEntry> = emptyList())

    @Serializable
    public data class PaymentsData(public val payments: List<PaymentEntry> = emptyList())

    @Serializable
    public data class MrcCharge(
        public val amount: Double? = null,
        public val description: String? = null,
    )

    @Serializable
    public data class MrcData(
        public val charges: List<MrcCharge> = emptyList(),
        public val total: Double? = null,
    )

    @Serializable
    public data class RegistrationData(
        public val agent: String? = null,
        public val uri: String? = null,
        public val expires: Int? = null,
    )

    /** POST /v2.2/account — admin-only sub-account creation. */
    @Serializable
    public data class AddRequest(
        public val username: Int? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val masterAccount: Int? = null,
    )

    @Serializable
    public data class AddData(
        public val username: String? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val masterAccount: String? = null,
        public val password: String? = null,
    )

    /** PUT /v2.2/account — partial update. */
    @Serializable
    public data class PutRequest(
        @SerialName("notify") public val notifyEnabled: Boolean? = null,
        public val notifyThreshold: Int? = null,
        public val timezone: String? = null,
        public val callerId: String? = null,
        public val e911: Boolean? = null,
        public val intl: Boolean? = null,
        public val sms: Boolean? = null,
        public val mms: Boolean? = null,
        public val ccs: Int? = null,
    )

    @Serializable
    public data class PutData(public val updated: List<String> = emptyList())

    /** POST /v2.2/accounts — public signup. */
    @Serializable
    public data class SignupRequest(
        public val name: String,
        public val email: String,
        public val promo: String? = null,
    )

    @Serializable
    public data class SignupData(
        public val username: String? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val password: String? = null,
    )

    /** POST /v2.2/account/recovery — no auth required. */
    @Serializable
    public data class RecoverRequest(public val email: String)

    @Serializable
    public data class RecoverData(public val message: String? = null)

    @Serializable
    public data class ApiKeyData(public val apikey: String)

    @Serializable
    internal data class ApiKeyRequest(val username: Int, val password: String)
}
