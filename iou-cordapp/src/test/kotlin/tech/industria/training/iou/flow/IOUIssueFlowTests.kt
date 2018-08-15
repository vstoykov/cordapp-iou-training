package tech.industria.training.iou.flow

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowException
import net.corda.finance.POUNDS
import net.corda.testing.core.singleIdentity
import org.junit.Test
import tech.industria.training.iou.IOUState
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IOUIssueFlowTests : IOUFlowTestsBase() {

    @Test
    fun `flow returns transaction signed by both parties`() {
        val signedTx = issueIOU(a, b, 10.POUNDS)
        network.waitQuiescent()
        requireThat {
            "Both parties should sign" using (
                    signedTx.sigs.map { it.by }.toSet() ==
                            listOf(a, b).map { it.info.singleIdentity().owningKey }.toSet())
        }
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