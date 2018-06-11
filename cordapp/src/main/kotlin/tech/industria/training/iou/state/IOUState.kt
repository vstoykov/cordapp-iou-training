package tech.industria.training.iou.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty


data class IOUState(val data: String) : ContractState {
    override val participants: List<AbstractParty> get() = listOf()
}