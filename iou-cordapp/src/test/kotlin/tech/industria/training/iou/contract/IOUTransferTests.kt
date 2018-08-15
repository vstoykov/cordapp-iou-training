package tech.industria.training.iou.contract

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.finance.POUNDS
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import tech.industria.training.iou.state.IOUState

class IOUTransferTests {
    protected val ledgerServices = MockServices(
        listOf("tech.industria.training.iou"),
        identityService = makeTestIdentityService(),
        initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "BG"))
    )
    protected val alice = TestIdentity(CordaX500Name("Alice", "", "BG"))
    protected val bob = TestIdentity(CordaX500Name("Bob", "", "BG"))
    protected val charlie = TestIdentity(CordaX500Name("Charlie", "", "BG"))
    protected val dan = TestIdentity(CordaX500Name("Dan", "", "BG"))
    protected val participants = listOf(alice.publicKey, bob.publicKey, charlie.publicKey)

    protected class DummyState : ContractState {
        override val participants: List<AbstractParty> get() = listOf()
    }

    protected val bobOwesAliceTen = IOUState(10.POUNDS, lender = alice.party, borrower = bob.party)
    protected val bobOwesCharlieTen = bobOwesAliceTen.copy(lender = charlie.party)

    @Test
    fun `successful transfer`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesCharlieTen)
                verifies()
            }
        }
    }

    @Test
    fun `transfer should have only one input`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                this `fails with` "An IOU transfer transaction should only consume one input state"
            }
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, IOUTransferTests.DummyState())
                input(IOUContract.PROGRAM_ID, IOUTransferTests.DummyState())
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                this `fails with` "An IOU transfer transaction should only consume one input state"
            }
        }
    }

    @Test
    fun `transfer should have only one output`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, IOUTransferTests.DummyState())
                this `fails with` "An IOU transfer transaction should only produce one output state"
            }
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, IOUTransferTests.DummyState())
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                this `fails with` "An IOU transfer transaction should only produce one output state"
            }
        }
    }

    @Test
    fun `the ammount and borrower should remain the same`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesCharlieTen.copy(amount = 5.POUNDS))
                failsWith("Only the lender property may change")
            }
            transaction {
                command(participants + listOf(dan.publicKey), IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesCharlieTen.copy(borrower = dan.party))
                failsWith("Only the lender property may change")
            }
        }
    }

    @Test
    fun `the lender should be different`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("The lender property must change in a transfer")
            }
        }
    }

    @Test
    fun `the new lender should not be the borrower`() {
        ledgerServices.ledger {
            transaction {
                command(participants, IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen.copy(lender = bob.party))
                failsWith("The new lender should not be the borrower")
            }
        }
    }

    @Test
    fun `all three parties should sign`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey), IOUContract.Commands.Transfer())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesCharlieTen)
                failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction")
            }
        }
    }
}
