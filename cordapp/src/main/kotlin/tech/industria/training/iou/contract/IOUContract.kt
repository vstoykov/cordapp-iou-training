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
        TODO()
    }
}