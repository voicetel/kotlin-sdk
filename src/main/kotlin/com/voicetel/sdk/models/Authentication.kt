package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** Authentication-resource models. */
public object Authentication {
    /** 0=Digest, 1=IP Auth, 2=Digest OR IP, 3=Digest AND IP. */
    public const val TYPE_DIGEST: Int = 0
    public const val TYPE_IP_AUTH: Int = 1
    public const val TYPE_DIGEST_OR_IP: Int = 2
    public const val TYPE_DIGEST_AND_IP: Int = 3

    @Serializable
    public data class PutRequest(
        public val authType: Int? = null,
        public val password: String? = null,
    )

    @Serializable
    public data class GetData(
        public val authType: Int? = null,
        public val authTypeDescription: String? = null,
        public val acl: List<CidrEntry> = emptyList(),
    )

    @Serializable
    public data class UpdatedEntry(
        public val field: String,
        public val value: Int? = null,
    )

    @Serializable
    public data class PutData(public val updated: List<UpdatedEntry> = emptyList())

    @Serializable
    public data class PutConflictData(public val updated: List<UpdatedEntry> = emptyList())
}
