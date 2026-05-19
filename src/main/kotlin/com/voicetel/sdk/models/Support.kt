package com.voicetel.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Support-resource models. */
public object Support {

    @Serializable
    public data class CreateRequest(
        public val subject: String,
        public val message: String,
        public val email: String? = null,
    )

    @Serializable
    public data class UpdateRequest(public val status: String)

    @Serializable
    public data class ReplyRequest(public val message: String)

    @Serializable
    public data class Source(
        public val via: String? = null,
        public val type: String? = null,
    )

    @Serializable
    public data class Action(
        public val text: String? = null,
        public val type: String? = null,
    )

    @Serializable
    public data class Actor(
        public val id: Int? = null,
        public val type: String? = null,
        public val email: String? = null,
        public val firstName: String? = null,
        public val lastName: String? = null,
        public val photoUrl: String? = null,
    )

    @Serializable
    public data class CustomFieldValue(
        public val id: Int? = null,
        public val value: String? = null,
        public val text: String? = null,
    )

    @Serializable
    public data class CustomerContactEntry(
        public val id: Int? = null,
        public val value: String? = null,
        public val type: String? = null,
    )

    @Serializable
    public data class CustomerWebsiteEntry(
        public val id: Int? = null,
        public val value: String? = null,
    )

    @Serializable
    public data class CustomerAddress(
        public val street: String? = null,
        public val city: String? = null,
        public val state: String? = null,
        public val country: String? = null,
        public val zip: String? = null,
    )

    @Serializable
    public data class CustomerEmbedded(
        public val address: CustomerAddress? = null,
        public val emails: List<CustomerContactEntry> = emptyList(),
        public val phones: List<CustomerContactEntry> = emptyList(),
        public val socialProfiles: List<CustomerContactEntry> = emptyList(),
        public val websites: List<CustomerWebsiteEntry> = emptyList(),
    )

    @Serializable
    public data class Attachment(
        public val id: Int? = null,
        public val mimeType: String? = null,
        public val fileName: String? = null,
        public val fileUrl: String? = null,
        public val size: Int? = null,
    )

    @Serializable
    public data class ThreadEmbedded(public val attachments: List<Attachment> = emptyList())

    @Serializable
    public data class Customer(
        public val id: Int? = null,
        public val firstName: String? = null,
        public val lastName: String? = null,
        public val email: String? = null,
        public val company: String? = null,
        public val jobTitle: String? = null,
        public val photoType: String? = null,
        public val photoUrl: String? = null,
        public val notes: String? = null,
        public val type: String? = null,
        public val createdAt: String? = null,
        public val updatedAt: String? = null,
        public val embedded: CustomerEmbedded? = null,
    )

    @Serializable
    public data class Thread(
        public val id: Int? = null,
        public val status: String? = null,
        public val state: String? = null,
        public val type: String? = null,
        public val body: String? = null,
        public val rating: Int? = null,
        public val ratingComment: String? = null,
        public val openedAt: String? = null,
        public val createdAt: String? = null,
        public val source: Source? = null,
        public val action: Action? = null,
        public val createdBy: Actor? = null,
        public val assignedTo: Actor? = null,
        public val customer: Customer? = null,
        public val to: List<String> = emptyList(),
        public val cc: List<String> = emptyList(),
        public val bcc: List<String> = emptyList(),
        public val embedded: ThreadEmbedded? = null,
    )

    @Serializable
    public data class ConversationEmbedded(public val threads: List<Thread> = emptyList())

    /**
     * A support ticket.
     *
     * Note: the wire field `number` is a ticket sequence number (e.g. 1015),
     * NOT a phone number. Exposed as [ticketNumber] with `@SerialName("number")`.
     */
    @Serializable
    public data class Conversation(
        public val id: Int? = null,
        @SerialName("number") public val ticketNumber: Int? = null,
        public val status: String? = null,
        public val state: String? = null,
        public val subject: String? = null,
        public val preview: String? = null,
        public val type: String? = null,
        public val mailboxId: Int? = null,
        public val folderId: Int? = null,
        public val threadsCount: Int? = null,
        public val closedBy: Int? = null,
        public val closedAt: String? = null,
        public val createdAt: String? = null,
        public val updatedAt: String? = null,
        public val userUpdatedAt: String? = null,
        public val customerWaitingSince: JsonElement? = null,
        public val source: Source? = null,
        public val createdBy: Actor? = null,
        public val assignee: Actor? = null,
        public val closedByUser: Actor? = null,
        public val customer: Customer? = null,
        public val cc: List<String> = emptyList(),
        public val bcc: List<String> = emptyList(),
        public val customFields: List<CustomFieldValue> = emptyList(),
        public val embedded: ConversationEmbedded? = null,
    )

    @Serializable
    public data class TicketData(public val ticket: Conversation)

    @Serializable
    public data class ListData(public val tickets: List<Conversation> = emptyList())

    @Serializable
    public data class ThreadsData(public val messages: List<Thread> = emptyList())

    @Serializable
    public data class ReplyData(public val message: String? = null)

    @Serializable
    public data class UpdateData(
        public val id: Int? = null,
        public val status: String? = null,
    )
}
