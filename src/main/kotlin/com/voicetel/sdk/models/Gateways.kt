package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** Gateways-resource models. */
public object Gateways {

    @Serializable
    public data class AddRequest(
        public val gateway: String,
        public val prefix: String? = null,
        public val limit: Int? = null,
    )

    @Serializable
    public data class UpdateRequest(
        public val gateway: String? = null,
        public val prefix: String? = null,
        public val limit: Int? = null,
    )

    /**
     * A single gateway row. `limit` is null for system routes.
     */
    @Serializable
    public data class Entry(
        public val id: Int? = null,
        public val gateway: String? = null,
        public val prefix: String? = null,
        public val limit: Int? = null,
        public val system: Boolean? = null,
    )

    /** One number bound to a gateway. */
    @Serializable
    public data class NumberSummary(
        public val number: String? = null,
        public val translated: String? = null,
        public val forward: Boolean? = null,
        public val forwardTo: String? = null,
        public val cnam: Boolean? = null,
        public val carrier: Int? = null,
        public val smsEnabled: Boolean? = null,
        public val faxEnabled: Boolean? = null,
    )

    @Serializable
    public data class ListData(public val gateways: List<Entry> = emptyList())

    @Serializable
    public data class NumbersData(public val numbers: List<NumberSummary> = emptyList())
}
