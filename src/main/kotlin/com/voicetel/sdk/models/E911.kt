package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/**
 * e911-resource models.
 *
 * Note: requests take a 10-digit `dn`; responses return the 11-digit
 * E.164 US form (country code 1 prepended).
 */
public object E911 {

    @Serializable
    public data class AddressRequest(
        public val address1: String,
        public val address2: String? = null,
        public val city: String,
        public val state: String,
        public val zip: String,
    )

    @Serializable
    public data class CreateRequest(
        public val dn: String,
        public val callername: String,
        public val address1: String,
        public val address2: String? = null,
        public val city: String,
        public val state: String,
        public val zip: String,
    )

    @Serializable
    public data class ProvisionByIdRequest(
        public val callername: String,
        public val addressid: Int,
    )

    @Serializable
    public data class Entry(
        public val dn: String? = null,
        public val callername: String? = null,
        public val address1: String? = null,
        public val address2: String? = null,
        public val city: String? = null,
        public val state: String? = null,
        public val zip: String? = null,
    )

    @Serializable
    public data class ValidatedAddress(
        public val addressid: Int? = null,
        public val address1: String? = null,
        public val address2: String? = null,
        public val city: String? = null,
        public val state: String? = null,
        public val zip: String? = null,
    )

    @Serializable
    public data class AllData(public val records: List<Entry> = emptyList())

    @Serializable
    public data class RecordData(public val record: Entry)

    @Serializable
    public data class ValidateData(public val address: ValidatedAddress)
}
