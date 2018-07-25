package tech.industria.training.iou.api

import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import tech.industria.training.iou.flow.IOUIssueFlow
import tech.industria.training.iou.state.IOUState
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Path("iou")
class IOUApi(val services: CordaRPCOps) {
    // Accessible at /api/iou/getEndpoint.
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun getIOUs() = services.vaultQueryBy<IOUState>().states

    @PUT
    @Path("issue")
    @Produces(MediaType.APPLICATION_JSON)
    fun issueIOUs(@QueryParam(value = "amount") amount: Int,
                  @QueryParam(value = "currency") currency: String,
                  @QueryParam(value = "party") party: String) : Response {

        val (status, message) = try {
            val lenderIdentity = services.wellKnownPartyFromX500Name(CordaX500Name.parse(party))
                    ?: throw IllegalStateException("Couldn't lookup node identity for $party.")
            val result = services.startFlowDynamic(
                    IOUIssueFlow.Initiator::class.java,
                    Amount(amount.toLong() * 100, Currency.getInstance(currency)),
                    lenderIdentity
            ).use { it.returnValue.getOrThrow() }
            Status.CREATED to result.tx.outputs.single()
        } catch (e: Exception) {
            Status.BAD_REQUEST to e.message
        }

        return Response
                .status(status)
                .entity(message)
                .build()
    }
}

