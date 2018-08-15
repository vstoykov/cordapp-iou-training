package tech.industria.training.iou.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

data class IOUState(
    val amount: Amount<Currency>,
    val lender: Party,
    val borrower: Party,
    val paid: Amount<Currency> = Amount(0, amount.token),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants get() = listOf(lender, borrower)
}