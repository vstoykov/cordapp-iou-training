package tech.industria.training.iou.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import tech.industria.training.iou.contract.IOUContract
import tech.industria.training.iou.state.IOUState
import java.math.BigDecimal
import java.util.*

class IOUSettleFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            private val linearId: UniqueIdentifier

    ) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val stateAndRef = getIOUByLinearId(linearId)
            val iou = stateAndRef.state.data

            val borrowerIdentity = serviceHub.identityService.requireWellKnownPartyFromAnonymous(iou.borrower)

            if (ourIdentity != borrowerIdentity) {
                throw FlowException("Settle IOU flow must be initiated by the borrower.")
            }

            val command = Command(IOUContract.Commands.Settle(), iou.participants.map { it.owningKey })

            val builder = TransactionBuilder(notary = notary)
                    .addInputState(stateAndRef)
                    .addCommand(command)

            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder)
            val sessions = (iou.participants - ourIdentity).map { initiateFlow(it) }.toSet()
            val stx = subFlow(CollectSignaturesFlow(ptx, sessions, CollectSignaturesFlow.tracker()))

            return subFlow(FinalityFlow(stx))
        }

        fun getIOUByLinearId(linearId: UniqueIdentifier): StateAndRef<IOUState> {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                    linearId = listOf(linearId),
                    status = Vault.StateStatus.UNCONSUMED
            )
            return serviceHub.vaultService.queryBy<IOUState>(queryCriteria).states.singleOrNull()
                    ?: throw FlowException("Obligation with id $linearId not found.")
        }
    }

    @InitiatedBy(IOUSettleFlow.Initiator::class)
    class Responder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                }
            }

            subFlow(signTransactionFlow)
        }
    }
}