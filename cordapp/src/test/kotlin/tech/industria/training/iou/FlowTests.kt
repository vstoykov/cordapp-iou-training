package tech.industria.training.iou

import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowException
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.industria.training.iou.flow.IOUIssueFlow
import tech.industria.training.iou.state.IOUState
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("tech.industria.training.iou"), threadPerNode = true)
        a = network.createNode()
        b = network.createNode()
        listOf(a, b).forEach {
            it.registerInitiatedFlow(IOUIssueFlow.Responder::class.java)
        }
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    protected fun issueIOU(initiator: StartedMockNode,
                            otherParty: StartedMockNode,
                            iouValue: Amount<Currency>) : SignedTransaction {
        val otherPartyIdentity = otherParty.info.legalIdentities.first()
        val flow = IOUIssueFlow.Initiator(iouValue, otherPartyIdentity)
        return initiator.startFlow(flow).getOrThrow()
    }

    @Test
    fun `amount should be positive`() {
        assertFailsWith<TransactionVerificationException> {
            issueIOU(a, b, 0.POUNDS)
        }
    }

    @Test
    fun `amount should not be too high`() {
        assertFailsWith<FlowException> {
            issueIOU(a, b, 101.POUNDS)
        }
    }

    @Test
    fun `successfully issue IOU`() {
        val signedTx = issueIOU(a, b, 10.POUNDS)
        network.waitQuiescent()

        val aIOU = a.services.loadState(signedTx.tx.outRef<IOUState>(0).ref).data as IOUState
        val bIOU = b.services.loadState(signedTx.tx.outRef<IOUState>(0).ref).data as IOUState

        assertEquals(aIOU, bIOU)
    }
}