package com.template

import com.template.flows.IssueNote
import com.template.flows.TransferNote
import com.template.states.NoteState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `that we can issue and transfer`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(bankB.name, partyAHandle.resolveName(bankB.name))
        assertEquals(bankA.name, partyBHandle.resolveName(bankA.name))

        val stateRef = partyAHandle.rpc.startFlow(::IssueNote, Amount.parseCurrency("1000.00 USD")).returnValue.getOrThrow()
        assertEquals(1_000_00, getNoteBalanceForNode(partyAHandle))
        partyAHandle.rpc.startFlow(::TransferNote, stateRef, partyBHandle.nodeInfo.legalIdentities.first()).returnValue.getOrThrow()
        assertEquals(0, getNoteBalanceForNode(partyAHandle))
        assertEquals(1_000_00, getNoteBalanceForNode(partyBHandle))
    }

    private fun getNoteBalanceForNode(node: NodeHandle): Long {
        println("getting balance for node ${node.nodeInfo.legalIdentities.first().name}")

        val qc = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val page = node.rpc.vaultQueryBy(
          criteria = qc,
          paging = PageSpecification(1, 100),
          sorting = Sort(emptySet()),
          contractStateType = NoteState::class.java
        )
        return page.states
          .apply {
              println("states:")
              this.forEach {
                  println(it)
              }
          }
          .map { it.state.data.amount.quantity }
          .fold(0L) { acc, value -> acc + value }
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
      DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
      .map { startNode(providedName = it.name) }
      .waitForAll()
}