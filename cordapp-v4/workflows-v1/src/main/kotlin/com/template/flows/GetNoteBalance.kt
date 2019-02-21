package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.NoteState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.loggerFor

@StartableByRPC
class GetNoteBalance : FlowLogic<Long>() {
  companion object {
    private val log = loggerFor<GetNoteBalance>()
  }
  @Suspendable
  override fun call(): Long {
    log.info("getting balance for node ${serviceHub.myInfo.legalIdentities.first().name}")

    val qc = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)

    return serviceHub.vaultService.queryBy(NoteState::class.java, qc)
      .states
      .apply {
        log.info("states:")
        this.forEach {
          log.info(it.toString())
        }
      }
      .map { it.state.data.amount.quantity }
      .fold(0L) { acc, value -> acc + value }
  }
}