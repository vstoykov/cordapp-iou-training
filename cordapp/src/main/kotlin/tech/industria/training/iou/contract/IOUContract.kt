package tech.industria.training.iou.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import tech.industria.training.iou.state.IOUState

class IOUContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "tech.industria.training.iou.contract.IOUContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
                "There should be one output state of type IOUContract.State." using (tx.outputs.size == 1)

                val out = tx.outputStates.single() as IOUState
                "The IOU's amount must be positive." using (out.amount > Amount(0, out.amount.token))
                "The lender and the borrower cannot be the same entity." using (out.lender != out.borrower)

                "There must be two signers." using (command.signers.size == 2)
                "The borrower and lender must be signers." using (
                        command.signers.toSet() == out.participants.map { it.owningKey }.toSet())
            }

            is Commands.Transfer -> requireThat {

            }
        }
    }
}