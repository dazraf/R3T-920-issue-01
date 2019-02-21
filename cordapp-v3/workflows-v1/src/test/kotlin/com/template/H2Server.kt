package com.template

import com.zaxxer.hikari.HikariDataSource
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.h2.tools.Server

class H2Server(network: MockNetwork, val nodes: List<StartedMockNode>, private val port : Int = 9092) {
  companion object {
    private val H2_NAME_RE = "^jdbc:h2:(mem:[^;:]+).*$".toRegex()
  }

  private val lock = java.lang.Object()
  private val server: Server = org.h2.tools.Server.createTcpServer("-tcpPort", port.toString(), "-tcpAllowOthers").start()
  private val webServer: Server = org.h2.tools.Server.createWebServer("-webPort", 9876.toString()).start()

  init {
    network.notaryNodes.first().writeJDBCEndpoint("notary")
    writeOutJDBCEndpoints()
  }

  /**
   * Block the current thread so that we can connect and examine the database
   */
  fun block() {
    writeOutJDBCEndpoints()
    synchronized(lock) {
      lock.wait()
    }
  }

  fun unblock() {
    lock.notify()
  }

  fun stop() {
    server.stop()
    webServer.stop()
  }

  private fun writeOutJDBCEndpoints() {
    nodes.forEach { it.writeJDBCEndpoint("party") }
  }

  @Suppress("UNCHECKED_CAST")
  private fun StartedMockNode.writeJDBCEndpoint(prefix: String) {
    val nodeField = StartedMockNode::class.java.getDeclaredField("node")
    nodeField.isAccessible = true
    val startedNode = nodeField.get(this) as net.corda.node.internal.StartedNode<net.corda.testing.node.internal.InternalMockNetwork.MockNode>

    val url = (startedNode.database.dataSource as HikariDataSource).dataSourceProperties.getProperty("url")

    val databaseName = H2_NAME_RE.matchEntire(url)?.groupValues?.get(1) ?: throw RuntimeException("could not extract db name from $url")
    println("Database for $prefix ${this.info.legalIdentities.first().name.organisation} uri: jdbc:h2:tcp://localhost:$port/$databaseName database: $databaseName")
  }
}