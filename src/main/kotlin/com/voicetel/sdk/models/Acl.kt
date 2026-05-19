package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** ACL-resource models. */
public object Acl {

    @Serializable
    public data class ModifyRequest(public val acl: List<CidrEntry>)

    @Serializable
    public data class ListData(public val acl: List<CidrEntry> = emptyList())

    @Serializable
    public data class AddData(public val added: List<CidrEntry> = emptyList())

    @Serializable
    public data class RemoveData(public val removed: List<CidrEntry> = emptyList())

    @Serializable
    public data class FailedEntry(
        public val cidr: String,
        public val reason: String? = null,
    )

    /** Data payload included in a 409 from POST/DELETE /v2.2/acl. */
    @Serializable
    public data class ConflictData(
        public val added: List<CidrEntry> = emptyList(),
        public val removed: List<CidrEntry> = emptyList(),
        public val failed: List<FailedEntry> = emptyList(),
    )
}
