package tech.industria.training.iou.flow

import net.corda.core.flows.FlowException
import net.corda.finance.POUNDS
import org.junit.Test
import tech.industria.training.iou.IOUState
import kotlin.test.assertFailsWith

class IOUSettleFlowTests : IOUFlowTestsBase() {

    @Test
    fun `only the borrower can initiate the settle flow`() {
        val issuanceTransaction = issueIOU(a, b, 10.POUNDS)
        network.waitQuiescent()
        val issuedIOU = issuanceTransaction.tx.outputStates.first() as IOUState

        assertFailsWith<FlowException> {
            settleIOU(b, issuedIOU.linearId)
        }
    }
}