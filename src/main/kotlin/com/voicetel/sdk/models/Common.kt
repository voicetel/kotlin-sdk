package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/**
 * A single CIDR row used by the ACL endpoint.
 *
 * Mask must be `/8`, `/16`, `/24`, or `/32` and must describe a routable
 * public address.
 */
@Serializable
public data class CidrEntry(
    public val cidr: String,
)
