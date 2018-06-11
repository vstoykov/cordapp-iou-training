package tech.industria.training.iou.api

import net.corda.core.messaging.CordaRPCOps
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("iou")
class IOUApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/iou/getEndpoint.
    @GET
    @Path("getEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {
        return Response.ok("{\"success\": true, \"data\": \"IOU Get Endpoint\" }").build()
    }
}
