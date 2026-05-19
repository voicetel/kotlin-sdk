package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** Lookup-resource models — CNAM and LRN dips. */
public object Lookups {

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
