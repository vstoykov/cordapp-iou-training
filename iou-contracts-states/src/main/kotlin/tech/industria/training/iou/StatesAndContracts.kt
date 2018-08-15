package tech.industria.training.iou

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import java.util.*


// *****************
// * Contract Code *
// *****************
class IOUContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "tech.industria.training.iou.IOUContract"
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
                "An IOU transfer transaction should only consume one input state" using (tx.inputs.size == 1)
                "An IOU transfer transaction should only produce one output state" using (tx.outputs.size == 1)

                val input = tx.inputStates.single() as IOUState
                val output = tx.outputStates.single() as IOUState
                "Only the lender property may change" using (input == output.copy(lender = input.lender))
                "The lender property must change in a transfer" using (input.lender != output.lender)
                "The new lender should not be the borrower" using (output.lender != output.borrower)
                "The borrower, old lender and new lender only must sign an IOU transfer transaction" using (
                        command.signers.toSet() == (
                                input.participants.map { it.owningKey }.toSet() union output.participants.map { it.owningKey }.toSet()))
            }

            is Commands.Settle -> requireThat {
                "An IOU settle transaction should only consume one input state" using (tx.inputs.size == 1)
                "An IOU settle transaction should not produce any output states" using (tx.outputs.size == 0)

                val input = tx.inputStates.single() as IOUState
                "An IOU settle transaction should be signed only by lender and borrower" using (
                        command.signers.toSet() == input.participants.map { it.owningKey }.toSet())
            }

            else -> throw IllegalArgumentException("Invalid command")
        }
    }
}

// *********
// * State *
// *********
data class IOUState(
        val amount: Amount<Currency>,
        val lender: Party,
        val borrower: Party,
        val paid: Amount<Currency> = Amount(0, amount.token),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants get() = listOf(lender, borrower)
}