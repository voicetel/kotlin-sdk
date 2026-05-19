package com.voicetel.sdk.models

import kotlinx.serialization.Serializable

/** Numbers-resource models. */
public object Numbers {

    // ---------------------------------------------------------- requests ---

    @Serializable
    public data class AddRequest(
        public val number: String,
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteRequest(public val route: Int)

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

    /** At least one of [routeIn] / [routeOut] must be set. */
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
        public val route: Int? = null,
    )

    @Serializable
    public data class PortOutPinUpdateRequest(public val pin: String)

    @Serializable
    public data class BulkUnassignRequest(public val numbers: List<String>)

    // ------------------------------------------------ entities & responses ---

    @Serializable
    public data class Detail(
        public val number: String? = null,
        public val translated: String? = null,
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

    @Serializable
    public data class MessagingState(
        public val number: String? = null,
        public val onAccount: Boolean? = null,
        public val enabled: Boolean? = null,
        public val carrier: Int? = null,
        public val routeIn: Int? = null,
        public val resource: String? = null,
        public val network: String? = null,
        public val campaign: CampaignBinding? = null,
    )

    @Serializable
    public data class AddData(
        public val number: String? = null,
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
        public val route: Int? = null,
    )

    @Serializable
    public data class RouteData(
        public val number: String? = null,
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

    @Serializable
    public data class MessagingCampaignAssignData(
        public val number: String? = null,
        public val campaignId: String? = null,
        public val carrier: Int? = null,
        public val network: String? = null,
        public val upstreamCnpId: String? = null,
        public val previousNetwork: String? = null,
        public val previousNetworkCleared: Boolean? = null,
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
    )
}
