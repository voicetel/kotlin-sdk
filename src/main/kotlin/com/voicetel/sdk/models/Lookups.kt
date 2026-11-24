package com.voicetel.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Lookup-resource models — CNAM, carrier/line-type, and LRN dips. */
public object Lookups {

    /**
     * Carrier and line-type details, with the field names used by the
     * underlying carrier lookup (hence snake_case on the wire for three of
     * them, mapped onto camelCase properties here).
     */
    @Serializable
    public data class CarrierInfo(
        /** Carrier or network name. */
        public val name: String? = null,
        /** Line type, such as voip, landline or mobile. */
        public val type: String? = null,
        /** Mobile country code, on mobile numbers. */
        @SerialName("mobile_country_code") public val mobileCountryCode: String? = null,
        /** Mobile network code, on mobile numbers. */
        @SerialName("mobile_network_code") public val mobileNetworkCode: String? = null,
        /** Error code when carrier details could not be determined. */
        @SerialName("error_code") public val errorCode: Int? = null,
    )

    /**
     * Data payload from GET /v2.2/carrier/{number}. Billed per lookup at the
     * account's CNAM rate, only when a result is returned.
     */
    @Serializable
    public data class CarrierData(
        public val number: String? = null,
        public val carrier: CarrierInfo? = null,
    )

    @Serializable
    public data class CnamData(
        public val cnam: String? = null,
        public val number: String? = null,
    )

    @Serializable
    public data class LrnData(
        public val lrn: String? = null,
        public val state: String? = null,
        public val city: String? = null,
        public val rc: String? = null,
        public val lata: String? = null,
        public val ocn: String? = null,
        public val lec: String? = null,
        public val lecType: String? = null,
        public val jurisdiction: String? = null,
        public val local: String? = null,
    )

    @Serializable
    public data class LrnLookupData(
        public val ani: String? = null,
        public val destination: String? = null,
        public val lrn: LrnData? = null,
    )
}
