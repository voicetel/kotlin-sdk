package com.voicetel.sdk.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Numbers-resource models. */
public object Numbers {

    // ---------------------------------------------------------- requests ---

    @Serializable
    public data class AddRequest(
        public val number: String,
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteRequest(
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int,
    )

    @Serializable
    public data class CnamRequest(public val enabled: Boolean)

    @Serializable
    public data class LidbRequest(
        public val cnam: String,
        public val customerOrderReference: String? = null,
    )

    @Serializable
    public data class FaxRequest(public val email: String)

    @Serializable
    public data class ForwardRequest(public val destination: String)

    @Serializable
    public data class TranslationRequest(public val translation: String)

    @Serializable
    public data class SmsRequest(
        public val type: String,
        public val resource: String,
    )

    /**
     * At least one of [routeIn] / [routeOut] must be set. [routeIn] is a
     * numbers_sms row id (0 to detach); [routeOut] is the outbound carrier id.
     */
    @Serializable
    public data class MessagingPatchRequest(
        public val routeIn: Int? = null,
        public val routeOut: Int? = null,
    )

    @Serializable
    public data class CampaignAssignRequest(public val campaignId: String)

    @Serializable
    public data class MoveRequest(
        public val accountId: Int,
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class PortOutPinUpdateRequest(public val pin: String)

    // ------------------------------------------------ entities & responses ---

    @Serializable
    public data class Detail(
        public val number: String? = null,
        public val translated: String? = null,
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
        public val gateway: String? = null,
        public val cnam: Boolean? = null,
        public val forward: Boolean? = null,
        public val forwardTo: String? = null,
        public val carrier: Int? = null,
        public val smsEnabled: Boolean? = null,
        public val faxEnabled: Boolean? = null,
    )

    @Serializable
    public data class CampaignBinding(
        public val id: String? = null,
        public val network: String? = null,
        public val status: String? = null,
        public val upstreamCnpId: String? = null,
    )

    /**
     * State of an asynchronous (path C) 10DLC enablement. The number keeps
     * its previous routing until [state] is `done`.
     */
    @Serializable
    public data class MessagingEnablementJob(
        public val campaignId: String? = null,
        /** queued -> hosting -> assigning -> routing -> done, or failed (see [message]). */
        public val state: String? = null,
        /** Latest progress note, or the reason for failure. */
        public val message: String? = null,
        public val createdAt: String? = null,
        public val updatedAt: String? = null,
    )

    @Serializable
    public data class MessagingState(
        public val number: String? = null,
        public val onAccount: Boolean? = null,
        public val enabled: Boolean? = null,
        public val carrier: Int? = null,
        public val routeIn: Int? = null,
        public val resource: String? = null,
        /** Messaging path the number is on: "A", "B", "C", or null. */
        public val network: String? = null,
        public val campaign: CampaignBinding? = null,
        /** Async path-C enablement in progress for this number, if any. */
        public val pending: MessagingEnablementJob? = null,
    )

    @Serializable
    public data class AddData(
        public val number: String? = null,
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class CnamData(
        public val number: String? = null,
        public val cnam: Boolean? = null,
    )

    @Serializable
    public data class FaxData(
        public val number: String? = null,
        public val email: String? = null,
    )

    @Serializable
    public data class ForwardData(
        public val number: String? = null,
        public val forwardTo: String? = null,
    )

    @Serializable
    public data class LidbData(
        public val number: String? = null,
        public val cnam: String? = null,
        public val customerOrderReference: String? = null,
        public val carrierStatus: String? = null,
    )

    @Serializable
    public data class MessagingPatchData(
        public val number: String? = null,
        public val updated: List<String> = emptyList(),
    )

    @Serializable
    public data class MoveData(
        public val number: String? = null,
        public val accountId: Int? = null,
        /** Global route type on the destination account after the move: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteData(
        public val number: String? = null,
        /** Global route type: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class SmsData(
        public val number: String? = null,
        public val type: String? = null,
        public val resource: String? = null,
    )

    @Serializable
    public data class TranslationData(
        public val number: String? = null,
        public val translation: String? = null,
    )

    /**
     * Result of binding a single number to a 10DLC campaign. [status] is
     * `enabled` (bound now, path B), `already` (the number was already bound
     * to this campaign; records reconciled, no new order), or `pending`
     * (accepted and being provisioned on path C — poll the number's messaging
     * state until the [job] reports `done` or `failed`).
     */
    @Serializable
    public data class MessagingCampaignAssignData(
        public val number: String? = null,
        public val campaignId: String? = null,
        /** "enabled", "already", or "pending". */
        public val status: String? = null,
        /** Outbound carrier id once enabled: 19 = path B, 1 = path C. */
        public val carrier: Int? = null,
        /** Messaging path the campaign resolves to: "B", "C", or null. */
        public val network: String? = null,
        public val upstreamCnpId: String? = null,
        /** Messaging path before this call: "A", "B", or null. */
        public val previousNetwork: String? = null,
        public val previousNetworkCleared: Boolean? = null,
        /** True when the number was already bound to this campaign. */
        public val alreadyAssigned: Boolean? = null,
        /** Network order id created for a path-B assignment (string or number); null otherwise. */
        public val orderId: JsonElement? = null,
        /** Non-fatal condition detected during the assignment, if any. */
        public val warning: String? = null,
        /** Human-readable note for a pending enablement. */
        public val message: String? = null,
        /** Async path-C enablement job; present when [status] is "pending". */
        public val job: MessagingEnablementJob? = null,
    )

    @Serializable
    public data class MessagingCampaignUnassignData(
        public val number: String? = null,
        public val campaignId: String? = null,
        public val network: String? = null,
        public val upstreamCnpId: String? = null,
        public val unassigned: Boolean? = null,
    )

    @Serializable
    public data class CampaignUnassignFailure(
        public val number: String,
        public val reason: String? = null,
    )

    @Serializable
    public data class BulkCampaignUnassignData(
        public val campaignId: String? = null,
        public val network: String? = null,
        public val upstreamCnpId: String? = null,
        public val unassignedNumbers: List<String> = emptyList(),
        public val failed: List<CampaignUnassignFailure> = emptyList(),
    )

    @Serializable
    public data class ListData(public val numbers: List<Detail> = emptyList())

    @Serializable
    public data class MessagingListData(public val numbers: List<MessagingState> = emptyList())

    @Serializable
    public data class PortOutPinUpdateData(
        public val number: String? = null,
        public val portOutPin: String? = null,
        /** True if this request set the PIN; false if one was already on file (locked for 30 days, returned unchanged). */
        public val changed: Boolean? = null,
    )
}
