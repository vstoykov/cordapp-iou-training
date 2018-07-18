package tech.industria.training.iou.contract

import net.corda.core.identity.CordaX500Name
import net.corda.finance.POUNDS
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import tech.industria.training.iou.state.IOUState

class IOUSettleTests {
    protected val ledgerServices = MockServices(
            listOf("tech.industria.training.iou"),
            identityService = makeTestIdentityService(),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "BG")))
    protected val dummyNotary = TestIdentity(DUMMY_NOTARY_NAME, 20);
    protected val alice = TestIdentity(CordaX500Name("Alice", "", "BG"))
    protected val bob = TestIdentity(CordaX500Name("Bob", "", "BG"))
    protected val charlie = TestIdentity(CordaX500Name("Charlie", "", "BG"))
    protected val participants = listOf(alice.publicKey, bob.publicKey)

    protected val bobOwesAliceTen = IOUState(10.POUNDS, lender = alice.party, borrower = bob.party)

    @Test
    fun `successful settle`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(participants, IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                verifies()
            }
        }
    }

    @Test
    fun `settle should have only one input`(){
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(participants, IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("An IOU settle transaction should only consume one input state")
            }
        }
    }

    @Test
    fun `settle should have no outputs`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(participants, IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                output(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("An IOU settle transaction should not produce any output states")
            }
        }
    }

    @Test
    fun `both borrower and lender should sign`() {
        ledgerServices.ledger(dummyNotary.party) {
            transaction {
                command(alice.publicKey, IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("An IOU settle transaction should be signed only by lender and borrower")
            }
            transaction {
                command(bob.publicKey, IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("An IOU settle transaction should be signed only by lender and borrower")
            }
            transaction {
                command(participants + listOf(charlie.publicKey), IOUContract.Commands.Settle())
                input(IOUContract.PROGRAM_ID, bobOwesAliceTen)
                failsWith("An IOU settle transaction should be signed only by lender and borrower")
            }
        }
    }
}