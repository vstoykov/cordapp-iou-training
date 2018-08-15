package tech.industria.training.iou.plugin

import java.util.function.Function
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import tech.industria.training.iou.api.IOUApi

class IOUWebPlugin : WebServerPluginRegistry {
    // A list of classes that expose web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::IOUApi))
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This template's web frontend is accessible at /web/template.
    override val staticServeDirs: Map<String, String> = mapOf(
        // This will serve the iou directory in resources to /web/iou
        "iou" to javaClass.classLoader.getResource("iou").toExternalForm()
    )
}