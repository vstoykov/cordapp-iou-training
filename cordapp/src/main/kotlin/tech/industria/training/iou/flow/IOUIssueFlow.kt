package tech.industria.training.iou.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import tech.industria.training.iou.contract.IOUContract
import tech.industria.training.iou.state.IOUState
import java.util.*

class IOUIssueFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            val iouValue: Amount<Currency>,
            val otherParty: Party

    ) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            val outputState = IOUState(iouValue, ourIdentity, otherParty)
            val cmd = Command(IOUContract.Commands.Issue(), listOf(ourIdentity.owningKey, otherParty.owningKey))

            val txBuilder = TransactionBuilder(notary = notary)
                    .addOutputState(outputState, IOUContract.PROGRAM_ID)
                    .addCommand(cmd)

            txBuilder.verify(serviceHub)

            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartySession = initiateFlow(otherParty)

            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is IOUState)
                    val iou = output as IOUState
                    "The IOU's amount can't be too high" using (iou.amount < Amount(100, iou.amount.token))
                }
            }

            subFlow(signTransactionFlow)
        }
    }
}