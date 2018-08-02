package tech.industria.training.iou

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.concurrent.transpose
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import tech.industria.training.iou.state.IOUState
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

class DriverBasedTest {
    val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `node test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts two nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (partyAHandle, partyBHandle) = listOf(
                startNode(providedName = bankA.name),
                startNode(providedName = bankB.name)
            ).transpose().getOrThrow()

            // This test makes an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate. This is a very basic test, in practice tests would be starting
            // flows, and verifying the states in the vault and other important metrics to ensure that your CorDapp is
            // working as intended.
            assertEquals(partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!.name, bankB.name)
            assertEquals(partyBHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!.name, bankA.name)
        }
    }

    @Test
    fun `node webserver api get ious`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val nodeHandles = listOf(
                startNode(providedName = bankA.name),
                startNode(providedName = bankB.name)
            ).transpose().getOrThrow()

            // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
            // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
            nodeHandles.forEach { nodeHandle ->
                val webserverHandle = startWebserver(nodeHandle).getOrThrow()

                val nodeAddress = webserverHandle.listenAddress
                val url = "http://$nodeAddress/api/iou/ious"

                val request = Request.Builder().url(url).build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()

                assertEquals(200, response.code())
                assertEquals("[ ]", response.body().string())
            }
        }
    }

    @Test
    fun `node webserver api issue ious`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (bankAHandle, _) = listOf(bankA, bankB).map {
                startNode(providedName = it.name)
            }.transpose().getOrThrow()

            val webserverHandle = startWebserver(bankAHandle).getOrThrow()
            val nodeAddress = webserverHandle.listenAddress
            val response = issueIOUApiCall(nodeAddress, "10", "BGN", bankB.name.toString())

            assertEquals(Status.CREATED.statusCode, response.code())

            val states = bankAHandle.rpc.vaultQueryBy<IOUState>().states
            assertEquals(1, states.size)

            val state = states.single().state.data
            val expected = JSONObject()
                .put(
                    "data", JSONObject()
                        .put("amount", "10.00 BGN")
                        .put("lender", bankB.name.toString())
                        .put("borrower", bankA.name.toString())
                        .put("paid", "0.00 BGN")
                        .put(
                            "linearId", JSONObject()
                                .put("externalId", null as Any?)
                                .put("id", state.linearId)
                        )
                        .put("participants", JSONArray(listOf(bankA.name.toString(), bankB.name.toString())))
                        .put("encumbrance", null as Any?)
                )
            JSONAssert.assertEquals(expected.toString(), response.body().string(), false)
        }
    }

    @Test
    fun `node webserver api bad issue data`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (bankAHandle, _) = listOf(bankA, bankB).map {
                startNode(providedName = it.name)
            }.transpose().getOrThrow()

            val webserverHandle = startWebserver(bankAHandle).getOrThrow()
            val nodeAddress = webserverHandle.listenAddress

            val responseInvalidX500Name = issueIOUApiCall(nodeAddress, "10", "BGN", "Invalid Party Name")
            assertEquals(Status.BAD_REQUEST.statusCode, responseInvalidX500Name.code())
            assertEquals("improperly specified input name: Invalid Party Name", responseInvalidX500Name.body().string())

            val responseMissingOrganisation = issueIOUApiCall(nodeAddress, "10", "BGN", "O=Missing Org, L=Gorno Nanadolnishte, C=BG")
            assertEquals(Status.BAD_REQUEST.statusCode, responseMissingOrganisation.code())
            assertEquals("couldn't lookup node identity for: O=Missing Org, L=Gorno Nanadolnishte, C=BG", responseMissingOrganisation.body().string())
        }
    }

    // *********************
    // * Utility Functions *
    // *********************

    fun issueIOUApiCall(nodeAddress: NetworkHostAndPort, amount: String, currency: String, party: String): Response {
        val uri = HttpUrl.parse("http://$nodeAddress/api/iou/issue")
            .newBuilder()
            .addQueryParameter("amount", amount)
            .addQueryParameter("currency", currency)
            .addQueryParameter("party", party)
            .build()
        val requestBody = RequestBody.create(MediaType.parse("application/json"), "")
        val request = Request.Builder().url(uri).post(requestBody).build()
        val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
        val response = client.newCall(request).execute()
        return response
    }
}