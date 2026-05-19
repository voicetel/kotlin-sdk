package com.voicetel.sdk.resources

import com.voicetel.sdk.Transport
import com.voicetel.sdk.models.INumbering
import io.ktor.http.HttpMethod
import kotlinx.serialization.KSerializer

/** Inventory searches, orders, and port-ins. */
public class INumberingService internal constructor(private val t: Transport) {

    /** Filters for [searchInventory]. */
    public class InventoryQuery(
        public var npa: Int? = null,
        public var nxx: Int? = null,
        public var state: String? = null,
        public var rateCenter: String? = null,
        public var contains: String? = null,
        public var endsWith: String? = null,
        public var limit: Int? = null,
    )

    /** Filters for [coverage]. */
    public class CoverageQuery(
        public var state: String? = null,
        public var rateCenter: String? = null,
    )

    public suspend fun searchInventory(q: InventoryQuery): INumbering.InventorySearchData {
        val m = mutableMapOf<String, Any?>()
        q.npa?.let { m["npa"] = it }
        q.nxx?.let { m["nxx"] = it }
        q.state?.let { m["state"] = it }
        q.rateCenter?.let { m["ratecenter"] = it }
        q.contains?.let { m["contains"] = it }
        q.endsWith?.let { m["endswith"] = it }
        q.limit?.let { m["limit"] = it }
        return t.request(HttpMethod.Get, "/v2.2/inventory", m, null, null,
            INumbering.InventorySearchData.serializer(), true)
    }

    public suspend fun coverage(q: CoverageQuery): INumbering.InventoryCoverageData {
        val m = mutableMapOf<String, Any?>()
        q.state?.let { m["state"] = it }
        q.rateCenter?.let { m["ratecenter"] = it }
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

    public suspend fun port(id: Int): INumbering.PortDetailData =
        t.request(HttpMethod.Get, "/v2.2/ports/$id", null, null, null,
            INumbering.PortDetailData.serializer(), true)

    public suspend fun submitPort(body: INumbering.PortSubmitRequest): INumbering.PortSubmitData =
        t.request(HttpMethod.Post, "/v2.2/ports", null,
            @Suppress("UNCHECKED_CAST") (INumbering.PortSubmitRequest.serializer() as KSerializer<Any>), body,
            INumbering.PortSubmitData.serializer(), true)

    public suspend fun portAvailability(number: String): INumbering.PortAvailabilityData =
        t.request(HttpMethod.Get, "/v2.2/ports/availability/$number", null, null, null,
            INumbering.PortAvailabilityData.serializer(), true)
}
