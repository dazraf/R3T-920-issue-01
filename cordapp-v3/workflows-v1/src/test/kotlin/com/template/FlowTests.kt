package com.template

import com.template.flows.IssueNote
import com.template.flows.TransferNote
import com.template.states.NoteState
import net.corda.core.contracts.Amount
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
  private val network = MockNetwork(listOf("com.template"))
  private val a = network.createNode()
  private val b = network.createNode()

  init {
    listOf(a, b).forEach {
      it.registerInitiatedFlow(TransferNote.Acceptor::class.java)
    }
  }

  @Before
  fun setup() {
    network.runNetwork()
  }

  @After
  fun tearDown() = network.stopNodes()

  @Test
  fun `that we can issue and transfer`() {
    val f1 = a.startFlow(IssueNote(Amount.parseCurrency("1000.00 USD"))).toCompletableFuture()
    network.runNetwork(1000)
    val stateRef = f1.getOrThrow()
    assertEquals(1_000_00, getNoteBalanceForNode(a))
    val f2 = a.startFlow(TransferNote(stateRef, b.info.legalIdentities.first())).toCompletableFuture()
    network.runNetwork(1000)
    f2.getOrThrow()
    assertEquals(0, getNoteBalanceForNode(a))
    assertEquals(1_000_00, getNoteBalanceForNode(b))
  }

  private fun getNoteBalanceForNode(node: StartedMockNode): Long {
    println("getting balance for node ${node.info.legalIdentities.first().name}")
    return node.transaction {
      val qc = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
      node.services.vaultService.queryBy(NoteState::class.java, qc)
        .states
        .apply {
          println("states:")
          this.forEach {
            println(it)
          }
        }
        .map { it.state.data.amount.quantity }
        .fold(0L) { acc, value -> acc + value }
    }
  }
}