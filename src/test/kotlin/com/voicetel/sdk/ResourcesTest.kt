package com.voicetel.sdk

import com.voicetel.sdk.models.Account
import com.voicetel.sdk.models.Acl
import com.voicetel.sdk.models.Authentication
import com.voicetel.sdk.models.CidrEntry
import com.voicetel.sdk.models.E911
import com.voicetel.sdk.models.Gateways
import com.voicetel.sdk.models.INumbering
import com.voicetel.sdk.models.Messaging
import com.voicetel.sdk.models.Numbers
import com.voicetel.sdk.models.Support
import com.voicetel.sdk.resources.INumberingService
import com.voicetel.sdk.resources.MessagingService
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class ResourcesTest {

    // -------------------------------------------------------------- account ---

    @Test
    fun accountGet() = runTest {
        val client = mockClient { req ->
            assertEquals("/v2.2/account", req.url.encodedPath)
            assertEquals(HttpMethod.Get, req.method)
            respond(envelope("""{"username":"u","name":"n","cash":12.5,"notify":true}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.get()
        assertEquals("u", d.username)
        assertEquals(12.5, d.cash)
        assertEquals(true, d.notifyEnabled)
        client.close()
    }

    @Test
    fun accountUpdate() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"updated":["timezone","ccs"]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.update(Account.PutRequest(timezone = "UTC", ccs = 4))
        assertEquals(2, d.updated.size)
        client.close()
    }

    @Test
    fun accountAdd() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"username":"1000000002","name":"sub","password":"pw"}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.add(Account.AddRequest(username = 1000000002, name = "sub", email = "x@y"))
        assertEquals("pw", d.password)
        client.close()
    }

    @Test
    fun accountSignup() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"username":"1","name":"n","password":"p"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.signup(Account.SignupRequest(name = "n", email = "e"))
        assertEquals("p", d.password)
        client.close()
    }

    @Test
    fun accountCdr() = runTest {
        val client = mockClient { req ->
            assertEquals("100", req.url.parameters["start"])
            assertEquals("200", req.url.parameters["end"])
            respond(envelope("""{"cdr":[],"start":100,"end":200}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.cdr(100, 200)
        assertEquals(100, d.start)
        client.close()
    }

    @Test
    fun accountCredits() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"credits":[{"date":"2026-01-01","paid":true,"amount":1.0}]}"""),
                HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.credits()
        assertEquals(1, d.credits.size)
        client.close()
    }

    @Test
    fun accountRecurringCharges() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"charges":[],"total":42.0}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.recurringCharges()
        assertEquals(42.0, d.total)
        client.close()
    }

    @Test
    fun accountPayments() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"payments":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.payments()
        assertTrue(d.payments.isEmpty())
        client.close()
    }

    @Test
    fun accountRegistration() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"agent":"a","uri":"sip:x","expires":60}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.registration()
        assertEquals(60, d.expires)
        client.close()
    }

    @Test
    fun accountRecover() = runTest {
        val client = mockClient(apiKey = "") { req ->
            // no auth required for recovery
            assertNull(req.headers["Authorization"])
            respond(envelope("""{"message":"ok"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.account.recover(Account.RecoverRequest(email = "e@x"))
        assertEquals("ok", d.message)
        client.close()
    }

    // ------------------------------------------------------------------ acl ---

    @Test
    fun aclList() = runTest {
        val client = mockClient { _ ->
            respond(envelope("""{"acl":[{"cidr":"1.2.3.0/24"}]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val d = client.acl.list()
        assertEquals(1, d.acl.size)
        client.close()
    }

    @Test
    fun aclAddRemove() = runTest {
        val client = mockClient { req ->
            when (req.method) {
                HttpMethod.Post -> respond(envelope("""{"added":[{"cidr":"1.0.0.0/24"}]}"""),
                    HttpStatusCode.OK, jsonHeaders)
                HttpMethod.Delete -> respond(envelope("""{"removed":[{"cidr":"1.0.0.0/24"}]}"""),
                    HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected method")
            }
        }
        val added = client.acl.add(Acl.ModifyRequest(listOf(CidrEntry("1.0.0.0/24"))))
        assertEquals(1, added.added.size)
        val removed = client.acl.remove(Acl.ModifyRequest(listOf(CidrEntry("1.0.0.0/24"))))
        assertEquals(1, removed.removed.size)
        client.close()
    }

    // ------------------------------------------------------- authentication ---

    @Test
    fun authGetAndUpdate() = runTest {
        val client = mockClient { req ->
            when (req.method) {
                HttpMethod.Get -> respond(envelope("""{"authType":1,"authTypeDescription":"IP","acl":[]}"""),
                    HttpStatusCode.OK, jsonHeaders)
                HttpMethod.Put -> respond(envelope("""{"updated":[{"field":"authType","value":1}]}"""),
                    HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected method")
            }
        }
        val get = client.authentication.get()
        assertEquals(1, get.authType)
        val put = client.authentication.update(Authentication.PutRequest(authType = 1))
        assertEquals(1, put.updated.size)
        client.close()
    }

    // ----------------------------------------------------------------- e911 ---

    @Test
    fun e911FullCycle() = runTest {
        val client = mockClient { req ->
            when {
                req.method == HttpMethod.Get && req.url.encodedPath == "/v2.2/e911" ->
                    respond(envelope("""{"records":[]}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Post && req.url.encodedPath == "/v2.2/e911" ->
                    respond(envelope("""{"record":{"dn":"12015551234"}}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Post && req.url.encodedPath == "/v2.2/e911/validations" ->
                    respond(envelope("""{"address":{"addressid":7,"city":"NYC"}}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Get && req.url.encodedPath.startsWith("/v2.2/e911/") ->
                    respond(envelope("""{"record":{"dn":"12015551234"}}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Put && req.url.encodedPath.startsWith("/v2.2/e911/") ->
                    respond(envelope("""{"record":{"dn":"12015551234","callername":"Acme"}}"""),
                        HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                else -> error("unexpected ${req.method} ${req.url.encodedPath}")
            }
        }
        client.e911.list()
        client.e911.create(E911.CreateRequest("2015551234", "Acme", "1 Main", null, "NYC", "NY", "10001"))
        val v = client.e911.validate(E911.AddressRequest("1 Main", null, "NYC", "NY", "10001"))
        assertEquals(7, v.address.addressid)
        client.e911.get("2015551234")
        val p = client.e911.provision("2015551234", E911.ProvisionByIdRequest("Acme", 7))
        assertEquals("Acme", p.record.callername)
        client.e911.remove("2015551234")
        client.close()
    }

    // ------------------------------------------------------------- gateways ---

    @Test
    fun gatewaysFull() = runTest {
        val client = mockClient { req ->
            when {
                req.method == HttpMethod.Get && req.url.encodedPath == "/v2.2/gateways" ->
                    respond(envelope("""{"gateways":[]}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Post && req.url.encodedPath == "/v2.2/gateways" ->
                    respond(envelope("""{"id":42,"gateway":"sip:gw"}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Get && req.url.encodedPath == "/v2.2/gateways/42" ->
                    respond(envelope("""{"id":42,"gateway":"sip:gw"}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Put && req.url.encodedPath == "/v2.2/gateways/42" ->
                    respond(envelope("""{"id":42,"gateway":"sip:gw2"}"""), HttpStatusCode.OK, jsonHeaders)
                req.method == HttpMethod.Delete && req.url.encodedPath == "/v2.2/gateways/42" ->
                    respond("", HttpStatusCode.NoContent)
                req.method == HttpMethod.Get && req.url.encodedPath == "/v2.2/gateways/42/numbers" ->
                    respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected ${req.url.encodedPath}")
            }
        }
        client.gateways.list()
        val a = client.gateways.add(Gateways.AddRequest("sip:gw"))
        assertEquals(42, a.id)
        client.gateways.get(42)
        val u = client.gateways.update(42, Gateways.UpdateRequest(gateway = "sip:gw2"))
        assertEquals("sip:gw2", u.gateway)
        client.gateways.remove(42)
        client.gateways.numbers(42)
        client.close()
    }

    // ------------------------------------------------------------ iNumbering --

    @Test
    fun iNumberingFull() = runTest {
        val client = mockClient { req ->
            when {
                req.url.encodedPath == "/v2.2/inventory" -> {
                    assertEquals("201", req.url.parameters["npa"])
                    respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
                }
                req.url.encodedPath == "/v2.2/inventory/coverage" ->
                    respond(envelope("""{"coverage":[]}"""), HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath == "/v2.2/orders" ->
                    respond(envelope("""{"orderId":"o1","amountCharged":1.0,"numbersOrdered":["2015551234"],"failed":[]}"""),
                        HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath == "/v2.2/ports" && req.method == HttpMethod.Get ->
                    respond(envelope("""{"ports":[]}"""), HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath == "/v2.2/ports" && req.method == HttpMethod.Post ->
                    respond(envelope("""{"pid":"P1","ticket":7,"message":"queued"}"""), HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath == "/v2.2/ports/9" ->
                    respond(envelope("""{"port":{"id":"9","status":"open"}}"""), HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath == "/v2.2/ports/availability/2015551234" ->
                    respond(envelope("""{"number":"2015551234","portable":true,"losingCarrier":"AT&T","localRoutingNumber":"2010000000","rateCenterTier":"A"}"""),
                        HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected ${req.url.encodedPath}")
            }
        }
        val q = INumberingService.InventoryQuery(npa = 201)
        client.iNumbering.searchInventory(q)
        client.iNumbering.coverage(INumberingService.CoverageQuery(state = "NJ"))
        val ord = client.iNumbering.order(INumbering.OrderCreateRequest(
            listOf(INumbering.OrderNumber.of("2015551234"))))
        assertEquals("o1", ord.orderId)
        client.iNumbering.ports()
        client.iNumbering.port(9)
        val ps = client.iNumbering.submitPort(
            INumbering.PortSubmitRequest(did = listOf("2015551234"), name = "X"))
        assertEquals(7, ps.ticket)
        val pa = client.iNumbering.portAvailability("2015551234")
        assertEquals("2010000000", pa.localRoutingNumber)
        assertEquals("A", pa.rateCenterTier)
        client.close()
    }

    @Test
    fun orderNumberObjectFormShape() {
        val plain = INumbering.OrderNumber.of("2015551234")
        val withRoute = INumbering.OrderNumber.of("2015551234", 5)
        assertEquals(plain, INumbering.OrderNumber.of("2015551234"))
        assertEquals(withRoute, INumbering.OrderNumber.of("2015551234", 5))
        // Not equal across forms.
        assertTrue(plain != withRoute)
    }

    // --------------------------------------------------------------- lookups --

    @Test
    fun lookups() = runTest {
        val client = mockClient { req ->
            when {
                req.url.encodedPath.startsWith("/v2.2/cnam/") ->
                    respond(envelope("""{"cnam":"Acme","number":"2015551234"}"""), HttpStatusCode.OK, jsonHeaders)
                req.url.encodedPath.startsWith("/v2.2/lrn/") ->
                    respond(envelope("""{"ani":"2012548000","destination":"2015551234","lrn":{"lrn":"3015551111"}}"""),
                        HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected")
            }
        }
        val c = client.lookups.cnam("2015551234")
        assertEquals("Acme", c.cnam)
        val l = client.lookups.lrn("2015551234", "2012548000")
        assertEquals("3015551111", l.lrn?.lrn)
        client.close()
    }

    // ----------------------------------------------------------- messaging --

    @Test
    fun messaging() = runTest {
        val client = mockClient { req ->
            val path = req.url.encodedPath
            when {
                path == "/v2.2/messages" && req.method == HttpMethod.Get ->
                    respond(envelope("""{"number":"2015551234","type":"sms","fromTs":1,"toTs":2,"messages":[]}"""),
                        HttpStatusCode.OK, jsonHeaders)
                path == "/v2.2/messages" && req.method == HttpMethod.Post ->
                    respond(envelope("""{"id":"m1","type":"sms","fromNumber":"f","toNumber":"t","parts":1}"""),
                        HttpStatusCode.OK, jsonHeaders)
                path == "/v2.2/messaging/brands" ->
                    respond(envelope("""{"result":{"statusCode":"OK","status":"pending"}}"""), HttpStatusCode.OK, jsonHeaders)
                path == "/v2.2/messaging/campaigns" && req.method == HttpMethod.Get ->
                    respond(envelope("""{"campaigns":[]}"""), HttpStatusCode.OK, jsonHeaders)
                path == "/v2.2/messaging/campaigns" && req.method == HttpMethod.Post ->
                    respond(envelope("""{"result":{"statusCode":"OK","status":"pending"}}"""), HttpStatusCode.OK, jsonHeaders)
                path == "/v2.2/numbers/messaging" -> {
                    assertEquals("2015551234,2015551235", req.url.parameters["numbers"])
                    respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
                }
                else -> error("unexpected $path")
            }
        }
        client.messaging.history(MessagingService.HistoryOptions.forNumber("2015551234"))
        client.messaging.send(Messaging.SendRequest("f", "t", "hi"))
        client.messaging.createBrand(Messaging.BrandCreateRequest("b", "B"))
        client.messaging.campaignStatus()
        client.messaging.createCampaign(Messaging.CampaignCreateRequest("b", "x", "desc"))
        client.messaging.numbersState(listOf("2015551234", "2015551235"))
        client.close()
    }

    @Test
    fun historyOptionsEmpty() = runTest {
        val client = mockClient { req ->
            assertTrue(req.url.parameters.isEmpty())
            respond(envelope("""{"messages":[]}"""), HttpStatusCode.OK, jsonHeaders)
        }
        client.messaging.history()
        client.close()
    }

    // ------------------------------------------------------------- numbers ---

    @Test
    fun numbersFull() = runTest {
        val client = mockClient { req ->
            val p = req.url.encodedPath
            val m = req.method
            when {
                p == "/v2.2/numbers" && m == HttpMethod.Get ->
                    respond(envelope("""{"numbers":[]}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers" && m == HttpMethod.Post ->
                    respond(envelope("""{"number":"2015551234","route":1}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234" && m == HttpMethod.Get ->
                    respond(envelope("""{"number":"2015551234","route":1}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234" && m == HttpMethod.Delete ->
                    respond("", HttpStatusCode.NoContent)
                p == "/v2.2/numbers/2015551234" && m == HttpMethod.Patch ->
                    respond(envelope("""{"number":"2015551234","accountId":2,"route":1}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/release" -> respond("", HttpStatusCode.NoContent)
                p == "/v2.2/numbers/2015551234/route" ->
                    respond(envelope("""{"number":"2015551234","route":2}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/translation" ->
                    respond(envelope("""{"number":"2015551234","translation":"sip:x"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/cnam" ->
                    respond(envelope("""{"number":"2015551234","cnam":true}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/lidb" ->
                    respond(envelope("""{"number":"2015551234","cnam":"Acme","carrierStatus":"ok"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/fax" && m == HttpMethod.Get ->
                    respond(envelope("""{"number":"2015551234","email":"f@x"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/fax" && m == HttpMethod.Put ->
                    respond(envelope("""{"number":"2015551234","email":"f@x"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/fax" && m == HttpMethod.Delete ->
                    respond("", HttpStatusCode.NoContent)
                p == "/v2.2/numbers/2015551234/forward" && m == HttpMethod.Put ->
                    respond(envelope("""{"number":"2015551234","forwardTo":"3015551111"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/forward" && m == HttpMethod.Delete ->
                    respond("", HttpStatusCode.NoContent)
                p == "/v2.2/numbers/2015551234/sms" && m == HttpMethod.Get ->
                    respond(envelope("""{"number":"2015551234","type":"webhook","resource":"r"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/sms" && m == HttpMethod.Put ->
                    respond(envelope("""{"number":"2015551234","type":"webhook","resource":"r"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/sms" && m == HttpMethod.Delete ->
                    respond("", HttpStatusCode.NoContent)
                p == "/v2.2/numbers/2015551234/messaging" && m == HttpMethod.Get ->
                    respond(envelope("""{"number":"2015551234","onAccount":true,"enabled":true}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/messaging" && m == HttpMethod.Patch ->
                    respond(envelope("""{"number":"2015551234","updated":["routeIn"]}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/messaging-campaign" && m == HttpMethod.Put ->
                    respond(envelope("""{"number":"2015551234","campaignId":"C1"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/messaging-campaign" && m == HttpMethod.Delete ->
                    respond(envelope("""{"number":"2015551234","campaignId":"C1","unassigned":true}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/messaging-campaign" && m == HttpMethod.Delete ->
                    respond(envelope("""{"campaignId":"C1","unassignedNumbers":["2015551234"],"failed":[]}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/numbers/2015551234/port-out-pin" ->
                    respond(envelope("""{"number":"2015551234","portOutPin":"1234"}"""), HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected ${req.method} $p")
            }
        }
        client.numbers.list()
        client.numbers.add(Numbers.AddRequest("2015551234"))
        client.numbers.get("2015551234")
        client.numbers.move("2015551234", Numbers.MoveRequest(accountId = 2))
        client.numbers.remove("2015551234")
        client.numbers.release("2015551234")
        client.numbers.setRoute("2015551234", Numbers.RouteRequest(2))
        client.numbers.setTranslation("2015551234", Numbers.TranslationRequest("sip:x"))
        client.numbers.setCnam("2015551234", Numbers.CnamRequest(true))
        client.numbers.setLidb("2015551234", Numbers.LidbRequest("Acme"))
        client.numbers.getFax("2015551234")
        client.numbers.setFax("2015551234", Numbers.FaxRequest("f@x"))
        client.numbers.removeFax("2015551234")
        client.numbers.setForward("2015551234", Numbers.ForwardRequest("3015551111"))
        client.numbers.removeForward("2015551234")
        client.numbers.getSms("2015551234")
        client.numbers.setSms("2015551234", Numbers.SmsRequest("webhook", "r"))
        client.numbers.removeSms("2015551234")
        client.numbers.getMessaging("2015551234")
        client.numbers.patchMessaging("2015551234", Numbers.MessagingPatchRequest(routeIn = 1))
        client.numbers.assignCampaign("2015551234", Numbers.CampaignAssignRequest("C1"))
        client.numbers.unassignCampaign("2015551234")
        client.numbers.bulkUnassignCampaign(listOf("2015551234"))
        client.numbers.setPortOutPin("2015551234", Numbers.PortOutPinUpdateRequest("1234"))
        client.close()
    }

    // ------------------------------------------------------------- support ---

    @Test
    fun supportFull() = runTest {
        val client = mockClient { req ->
            val p = req.url.encodedPath
            val m = req.method
            when {
                p == "/v2.2/support/tickets" && m == HttpMethod.Get ->
                    respond(envelope("""{"tickets":[]}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/support/tickets" && m == HttpMethod.Post ->
                    respond(envelope("""{"ticket":{"id":1,"number":1015,"subject":"s"}}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/support/tickets/1" && m == HttpMethod.Get ->
                    respond(envelope("""{"ticket":{"id":1,"number":1015}}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/support/tickets/1" && m == HttpMethod.Put ->
                    respond(envelope("""{"id":1,"status":"closed"}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/support/tickets/1" && m == HttpMethod.Delete ->
                    respond("", HttpStatusCode.NoContent)
                p == "/v2.2/support/tickets/1/messages" ->
                    respond(envelope("""{"messages":[]}"""), HttpStatusCode.OK, jsonHeaders)
                p == "/v2.2/support/tickets/1/replies" ->
                    respond(envelope("""{"message":"replied"}"""), HttpStatusCode.OK, jsonHeaders)
                else -> error("unexpected ${req.method} $p")
            }
        }
        client.support.list()
        val td = client.support.create(Support.CreateRequest("s", "m"))
        assertEquals(1015, td.ticket.ticketNumber)
        client.support.get(1)
        client.support.update(1, Support.UpdateRequest("closed"))
        client.support.delete(1)
        client.support.messages(1)
        client.support.reply(1, Support.ReplyRequest("hello"))
        client.close()
    }

    // ------------------------------------------------------------- errors ---

    @Test
    fun numbersGetNotFound() = runTest {
        val client = mockClient { _ ->
            respond("""{"error":"missing"}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> { client.numbers.get("9999999999") }
        assertTrue(err.isNotFound)
        client.close()
    }

    @Test
    fun aclConflictBodyPreserved() = runTest {
        val client = mockClient { _ ->
            respond(
                """{"error":"conflict","data":{"failed":[{"cidr":"x","reason":"y"}]}}""",
                HttpStatusCode.Conflict, jsonHeaders)
        }
        val err = assertFailsWith<ApiError> {
            client.acl.add(Acl.ModifyRequest(listOf(CidrEntry("1.0.0.0/24"))))
        }
        assertNotNull(err.body)
        assertTrue(err.isConflict)
        client.close()
    }
}
