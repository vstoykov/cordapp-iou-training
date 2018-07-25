package tech.industria.training.iou

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import okhttp3.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
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
            ).map { it.getOrThrow() }

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
            ).map { it.getOrThrow() }

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
            val bankAHandle = startNode(providedName = bankA.name).getOrThrow()
            startNode(providedName = bankB.name).getOrThrow()

            val webserverHandle = startWebserver(bankAHandle).getOrThrow()
            val nodeAddress = webserverHandle.listenAddress
            val url = "http://$nodeAddress/api/iou/issue?amount=10&currency=BGN&party=${bankB.name}"
            val requestBody = RequestBody.create(MediaType.parse("application/json"), "")
            val request = Request.Builder().url(url).put(requestBody).build()
            val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
            val response = client.newCall(request).execute()

            assertEquals(Response.Status.CREATED.statusCode, response.code())
            // TODO: Test the response
            //assertEquals("Transaction id 1 sent to counterparty.", response.body().string())
        }
    }
}