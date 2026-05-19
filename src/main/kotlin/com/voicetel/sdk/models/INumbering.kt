package com.voicetel.sdk.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** iNumbering-resource models — inventory, orders, port-ins. */
public object INumbering {

    @Serializable
    public data class OrderNumberSpec(
        public val number: String,
        public val route: Int? = null,
    )

    /**
     * Single entry in [OrderCreateRequest.numbers]. Use [of] for a plain
     * TN string, or the two-arg [of] for a `{number, route}` object.
     */
    @Serializable(with = OrderNumberSerializer::class)
    public class OrderNumber private constructor(
        internal val plain: String?,
        internal val spec: OrderNumberSpec?,
    ) {
        public companion object {
            public fun of(number: String): OrderNumber = OrderNumber(number, null)
            public fun of(number: String, route: Int?): OrderNumber =
                if (route == null) OrderNumber(number, null)
                else OrderNumber(null, OrderNumberSpec(number, route))
        }

        override fun equals(other: Any?): Boolean =
            other is OrderNumber && other.plain == plain && other.spec == spec
        override fun hashCode(): Int = (plain?.hashCode() ?: 0) * 31 + (spec?.hashCode() ?: 0)
        override fun toString(): String = plain ?: spec.toString()
    }

    public object OrderNumberSerializer : KSerializer<OrderNumber> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("OrderNumber", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: OrderNumber) {
            require(encoder is JsonEncoder) { "OrderNumber requires JSON encoder" }
            if (value.plain != null) {
                encoder.encodeJsonElement(JsonPrimitive(value.plain))
            } else {
                val spec = value.spec!!
                val obj = buildJsonObject {
                    put("number", spec.number)
                    if (spec.route != null) put("route", spec.route)
                }
                encoder.encodeJsonElement(obj)
            }
        }

        override fun deserialize(decoder: Decoder): OrderNumber {
            require(decoder is JsonDecoder) { "OrderNumber requires JSON decoder" }
            return when (val el = decoder.decodeJsonElement()) {
                is JsonPrimitive -> OrderNumber.of(el.content)
                is JsonObject -> {
                    val n = (el["number"] as? JsonPrimitive)?.content
                        ?: error("OrderNumber object missing 'number'")
                    val r = (el["route"] as? JsonPrimitive)?.content?.toIntOrNull()
                    OrderNumber.of(n, r)
                }
                else -> error("OrderNumber must be string or object, got $el")
            }
        }
    }

    @Serializable
    public data class OrderCreateRequest(public val numbers: List<OrderNumber>)

    @Serializable
    public data class PortFeatureLidb(public val name: String)

    @Serializable
    public data class PortFeatureRouting(public val gatewayId: Int)

    @Serializable
    public data class PortFeatureSms(public val campaignId: String)

    @Serializable
    public data class PortFeature(
        public val number: String,
        public val routing: PortFeatureRouting? = null,
        public val lidb: PortFeatureLidb? = null,
        public val sms: PortFeatureSms? = null,
    )

    @Serializable
    public data class PortSubmitRequest(
        public val did: List<String>,
        public val name: String,
        public val nameType: String? = null,
        public val lcBtn: String? = null,
        public val lcAccountNumber: String? = null,
        public val streetNumber: String? = null,
        public val street: String? = null,
        public val streetType: String? = null,
        public val city: String? = null,
        public val state: String? = null,
        public val zip: String? = null,
        public val country: String? = null,
        public val authPerson: String? = null,
        public val streetPrefix: String? = null,
        public val streetSuffix: String? = null,
        public val floor: String? = null,
        public val room: String? = null,
        public val building: String? = null,
        public val unitValue: String? = null,
        public val desiredDueDate: String? = null,
        public val pin: String? = null,
        public val features: List<PortFeature>? = null,
    )

    @Serializable
    public data class InventoryItem(
        public val number: String? = null,
        public val rateCenter: String? = null,
        public val city: String? = null,
        public val province: String? = null,
        public val lata: String? = null,
    )

    @Serializable
    public data class InventoryCoverageItem(
        public val count: Int? = null,
        public val npa: String? = null,
        public val nxx: String? = null,
        public val block: String? = null,
        public val city: String? = null,
        public val rcAbbre: String? = null,
        public val lata: String? = null,
        public val locState: String? = null,
    )

    @Serializable
    public data class PortSummary(
        public val status: String? = null,
        public val id: String? = null,
        public val pid: String? = null,
        public val foc: String? = null,
        public val createdAt: String? = null,
        public val message: String? = null,
        public val supportUrl: String? = null,
    )

    @Serializable
    public data class PortDetail(
        public val status: String? = null,
        public val id: String? = null,
        public val pid: String? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val foc: String? = null,
        public val createdAt: String? = null,
        public val numbers: List<String> = emptyList(),
        public val message: String? = null,
    )

    @Serializable
    public data class InventorySearchData(public val numbers: List<InventoryItem> = emptyList())

    @Serializable
    public data class InventoryCoverageData(public val coverage: List<InventoryCoverageItem> = emptyList())

    @Serializable
    public data class OrderFailedEntry(
        public val number: String,
        public val reason: String? = null,
    )

    @Serializable
    public data class OrderCreateData(
        public val orderId: String? = null,
        public val amountCharged: Double? = null,
        public val numbersOrdered: List<String> = emptyList(),
        public val failed: List<OrderFailedEntry> = emptyList(),
    )

    @Serializable
    public data class PortListData(public val ports: List<PortSummary> = emptyList())

    @Serializable
    public data class PortDetailData(public val port: PortDetail)

    @Serializable
    public data class PortSubmitData(
        public val pid: String? = null,
        public val ticket: Int? = null,
        public val message: String? = null,
        public val loaUrl: String? = null,
        public val portUrl: String? = null,
    )

    /** v2.2.10 added `localRoutingNumber` and `rateCenterTier`. */
    @Serializable
    public data class PortAvailabilityData(
        public val number: String? = null,
        public val portable: Boolean? = null,
        public val losingCarrier: String? = null,
        public val localRoutingNumber: String? = null,
        public val rateCenterTier: String? = null,
        public val reason: String? = null,
    )
}
