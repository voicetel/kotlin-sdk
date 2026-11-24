package com.voicetel.sdk.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Numbers-resource models. */
public object Numbers {

    // ---------------------------------------------------------- requests ---

    @Serializable
    public data class AddRequest(
        public val number: String,
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. An unknown route is rejected with 400. */
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteRequest(
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. An unknown route is rejected with 400. */
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
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class PortOutPinUpdateRequest(public val pin: String)

    // ------------------------------------------------- directory listing (411) ---

    /**
     * A 411 directory listing as published (or, on PUT, as submitted).
     * [type] "B" carries the business name in [name]; "R" carries
     * [firstName] / [lastName]. All address fields may be empty strings.
     * [complex] marks a multi-line listing, which is read-only through the
     * API.
     */
    @Serializable
    public data class DirectoryListing(
        /** B (business) or R (residence). */
        public val type: String? = null,
        /** Business name (type B). */
        public val name: String? = null,
        /** Resident's first name (type R). */
        public val firstName: String? = null,
        /** Resident's last name (type R). */
        public val lastName: String? = null,
        public val streetNumber: String? = null,
        public val streetName: String? = null,
        /** A word such as St, Ave or Rd. */
        public val streetType: String? = null,
        /** Street direction: "", N, S, E, W, NE, NW, SE or SW. */
        public val streetSuffix: String? = null,
        /** Suite or unit. */
        public val location: String? = null,
        public val city: String? = null,
        /** Two-letter US state or territory code. */
        public val state: String? = null,
        /** 5-digit ZIP. */
        public val zip: String? = null,
        /** True for a multi-line listing; read-only through the API. */
        public val complex: Boolean? = null,
    )

    /**
     * PUT /v2.2/numbers/{number}/directory-listing — add or replace the
     * listing. [type] is `business` or `residence` (the shorthand `B` and
     * `R` is also accepted by the server). A business sets [name]; a
     * residence sets [firstName] and [lastName]. The street is optional —
     * give [streetNumber] and [streetName] together, or neither;
     * [streetSuffix] and [location] require a street name. [city], [state]
     * (two-letter US state or territory code) and [zip] (5 digits; a ZIP+4 is
     * accepted and stored as the 5-digit ZIP) are required.
     */
    @Serializable
    public data class DirectoryListingRequest(
        /** business or residence; the shorthand B and R is also accepted. */
        public val type: String,
        /** Business name; required for business (<=60). */
        public val name: String? = null,
        /** Resident's first name; required for residence (<=30). */
        public val firstName: String? = null,
        /** Resident's last name; required for residence (<=30). */
        public val lastName: String? = null,
        /** Up to 10 letters or digits; give it together with [streetName], or neither. */
        public val streetNumber: String? = null,
        /** Street name; give it together with [streetNumber], or neither. */
        public val streetName: String? = null,
        /** A word such as St, Ave or Rd. */
        public val streetType: String? = null,
        /** Street direction; requires a street name. */
        public val streetSuffix: String? = null,
        /** Suite or unit; requires a street name. */
        public val location: String? = null,
        public val city: String,
        public val state: String,
        public val zip: String,
    )

    /**
     * Data payload from GET/PUT/DELETE /v2.2/numbers/{number}/directory-listing.
     * PUT and DELETE are accepted (202) and applied in the background:
     * [status] is `pending` / `removing` (or `not_listed` when a pending add
     * was cancelled) and [orderId] carries the carrier order reference. GET
     * reports the state as published, with [eligible] telling whether a
     * listing can be added.
     */
    @Serializable
    public data class DirectoryListingData(
        public val number: String? = null,
        /** listed: published. not_listed: none. pending: an add or change is being applied. removing: a removal is being applied. unavailable: the number cannot be listed. */
        public val status: String? = null,
        /** Whether a listing can be added to this number (GET). */
        public val eligible: Boolean? = null,
        /** The published listing, or null. */
        public val listing: DirectoryListing? = null,
        /** A submitted listing not yet published, or null. */
        public val pending: DirectoryListing? = null,
        /** Reference of the submitted change (PUT and DELETE), or null. */
        public val orderId: String? = null,
    )

    // ------------------------------------------------ entities & responses ---

    @Serializable
    public data class Detail(
        public val number: String? = null,
        public val translated: String? = null,
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
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
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
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
        /** A gateway id on this account (GET /v2.2/gateways) or a global route on the destination account after the move: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteData(
        public val number: String? = null,
        /** A gateway id on this account (GET /v2.2/gateways) or a global route: 1 USER, 2 T30, 3 VOICE_BRIDGE, 4 DID (default), 5 T38, 6 VOICEML, 8 SDN, 9 MOBILE. */
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
