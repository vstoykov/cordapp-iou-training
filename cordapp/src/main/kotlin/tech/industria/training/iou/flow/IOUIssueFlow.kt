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
import java.math.BigDecimal
import java.util.*

class IOUIssueFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            val iouValue: Amount<Currency>,
            val otherParty: Party

    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TX : ProgressTracker.Step("Generating transaction.")
            object VERIFING_TX: ProgressTracker.Step("Verifying contract.")
            object COLLECTING_TX_SIGNATURES: ProgressTracker.Step("Collecting signatures.")
            object FINALIZING_TX: ProgressTracker.Step("Finalizing transaction.")

            fun tracker() = ProgressTracker(GENERATING_TX, VERIFING_TX, COLLECTING_TX_SIGNATURES, FINALIZING_TX)
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep = GENERATING_TX
            System.out.printf("iouValue = %s", iouValue)
            val outputState = IOUState(iouValue, ourIdentity, otherParty)
            val issueCommand = Command(IOUContract.Commands.Issue(), outputState.participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary = notary)
                    .addOutputState(outputState, IOUContract.PROGRAM_ID)
                    .addCommand(issueCommand)

            progressTracker.currentStep = VERIFING_TX

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = COLLECTING_TX_SIGNATURES

            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // In this case is equivalent to listOf(initiateFlow(otherParty)), but this is more generic approach
            val sessions = (outputState.participants - ourIdentity).map { initiateFlow(it) }.toSet()

            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, sessions, CollectSignaturesFlow.tracker()))

            progressTracker.currentStep = FINALIZING_TX

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
                    System.out.printf("iou.amount = %s", iou.amount)
                    "The IOU's amount can't be too high" using (iou.amount.toDecimal() < BigDecimal(100))
                }
            }

            subFlow(signTransactionFlow)
        }
    }
}