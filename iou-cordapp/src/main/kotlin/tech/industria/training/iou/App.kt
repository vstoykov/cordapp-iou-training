package tech.industria.training.iou

import net.corda.core.serialization.SerializationWhitelist


// Serialization whitelist.
class IOUSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>> = listOf(IOUData::class.java)
}

// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
// we want to send it to other nodes within a flow.
data class IOUData(val payload: String)
