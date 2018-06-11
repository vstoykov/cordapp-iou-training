package tech.industria.training.iou.contract

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.finance.POUNDS
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import tech.industria.training.iou.state.IOUState

class IOUIssueTests {
    protected val ledgerServices = MockServices(
            listOf("tech.industria.training.iou"),
            identityService = makeTestIdentityService(),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "BG")))
    protected val dummyNotary = TestIdentity(DUMMY_NOTARY_NAME, 20);
    protected val alice = TestIdentity(CordaX500Name("Alice", "", "BG"))
    protected val bob = TestIdentity(CordaX500Name("Bob", "", "BG"))
    protected val charlie = TestIdentity(CordaX500Name("Charlie", "", "BG"))

    protected class DummyState : ContractState {
        override val participants: List<AbstractParty> get() = listOf()
    }

    protected val tenFromAliceToBob = IOUState(10.POUNDS, alice.party, bob.party)
    protected val fiveFromBobToCharlie = IOUState(10.POUNDS, bob.party, charlie.party)

    @Test
    fun `create transaction must have no inputs`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                input(IOUContract.PROGRAM_ID, DummyState())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                this `fails with` "No inputs should be consumed when issuing an IOU."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                verifies()
            }
        }
    }

    @Test
    fun `create transaction must have one output`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                output(IOUContract.PROGRAM_ID, IOUState(5.POUNDS, alice.party, bob.party))
                this `fails with` "There should be one output state of type IOUContract.State."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                verifies()
            }
        }
    }

    @Test
    fun `cannot create transaction with negative or zero value`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, IOUState(0.POUNDS, alice.party, bob.party))
                this `fails with` "The IOU's amount must be positive."
            }
//            transaction {
//                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Create())
//                output(IOUContract.PROGRAM_ID, IOUContract.State((-1).POUNDS, alice.party, bob.party))
//                this `fails with` "The IOU's amount must be positive."
//            }
        }
    }

    @Test
    fun `there should be only two signers to create transaction`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                this `fails with` "There must be two signers."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, tenFromAliceToBob)
                this `fails with` "There must be two signers."
            }
        }
    }

    @Test
    fun `lender and borrower must sign create transaction`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, fiveFromBobToCharlie)
                this `fails with` "The borrower and lender must be signers."
            }
        }
    }

    @Test
    fun `lender and borrower cannot be the same`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), IOUContract.Commands.Issue())
                output(IOUContract.PROGRAM_ID, IOUState(10.POUNDS, alice.party, alice.party))
                this `fails with` "The lender and the borrower cannot be the same entity."
            }
        }
    }
}