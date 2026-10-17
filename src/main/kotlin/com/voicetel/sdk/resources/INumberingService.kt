package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.INumbering
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.KSerializer

/** Inventory searches, orders, and port-ins. */
public class INumberingService internal constructor(private val t: Transport) {

    /**
     * Filters for [searchInventory]. [tnMask] (prefix or wildcard pattern,
     * e.g. `201`, `201555`, `201555*`) is required.
     */
    public class InventoryQuery(
        public val tnMask: String,
        public var quantity: Int? = null,
        public var province: String? = null,
        public var rateCenter: String? = null,
        public var lata: String? = null,
    )

    /** Aggregation dimension for [coverage]. */
    public enum class CountBy(public val wire: String) {
        STATE("state"),
        CITY("city"),
        RATE_CENTER("rateCenter"),
        NPANXX("npanxx"),
        BLOCK("block"),
        ;

        override fun toString(): String = wire
    }

    /** Filters for [coverage]. [countBy] is required. */
    public class CoverageQuery(
        public val countBy: CountBy,
        public var locState: String? = null,
        public var city: String? = null,
        public var rcAbbre: String? = null,
        public var npa: String? = null,
        public var lata: String? = null,
    )

    public suspend fun searchInventory(q: InventoryQuery): INumbering.InventorySearchData {
        val m = mutableMapOf<String, Any?>()
        m["tnMask"] = q.tnMask
        q.quantity?.let { m["quantity"] = it }
        q.province?.let { m["province"] = it }
        q.rateCenter?.let { m["rateCenter"] = it }
        q.lata?.let { m["lata"] = it }
        return t.request(HttpMethod.Get, "/v2.2/inventory", m, null, null,
            INumbering.InventorySearchData.serializer(), true)
    }

    public suspend fun coverage(q: CoverageQuery): INumbering.InventoryCoverageData {
        val m = mutableMapOf<String, Any?>()
        m["countBy"] = q.countBy.wire
        q.locState?.let { m["locState"] = it }
        q.city?.let { m["city"] = it }
        q.rcAbbre?.let { m["rcAbbre"] = it }
        q.npa?.let { m["npa"] = it }
        q.lata?.let { m["lata"] = it }
        return t.request(HttpMethod.Get, "/v2.2/inventory/coverage", m, null, null,
            INumbering.InventoryCoverageData.serializer(), true)
    }

    /** Purchase new TNs. */
    public suspend fun order(body: INumbering.OrderCreateRequest): INumbering.OrderCreateData =
        t.request(HttpMethod.Post, "/v2.2/orders", null,
            @Suppress("UNCHECKED_CAST") (INumbering.OrderCreateRequest.serializer() as KSerializer<Any>), body,
            INumbering.OrderCreateData.serializer(), true)

    public suspend fun ports(): INumbering.PortListData =
        t.request(HttpMethod.Get, "/v2.2/ports", null, null, null,
            INumbering.PortListData.serializer(), true)

    /**
     * Get a single port-in record. [id] is the 5-character alphanumeric pid
     * from the list response's `pid` field.
     */
    public suspend fun port(id: String): INumbering.PortDetailData =
        t.request(HttpMethod.Get, "/v2.2/ports/${id.encodeURLPathPart()}", null, null, null,
            INumbering.PortDetailData.serializer(), true)

    public suspend fun submitPort(body: INumbering.PortSubmitRequest): INumbering.PortSubmitData =
        t.request(HttpMethod.Post, "/v2.2/ports", null,
            @Suppress("UNCHECKED_CAST") (INumbering.PortSubmitRequest.serializer() as KSerializer<Any>), body,
            INumbering.PortSubmitData.serializer(), true)

    public suspend fun portAvailability(number: String): INumbering.PortAvailabilityData =
        t.request(HttpMethod.Get, "/v2.2/ports/availability/${number.encodeURLPathPart()}", null, null, null,
            INumbering.PortAvailabilityData.serializer(), true)
}
