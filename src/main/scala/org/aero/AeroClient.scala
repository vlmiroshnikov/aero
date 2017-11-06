package org.aero

import com.aerospike.client.AerospikeClient
import com.aerospike.client.async._
import com.aerospike.client.policy.ClientPolicy

object AeroClient {
  def apply(host: String, port: Int): AeroClient = new AeroClient {
    private val cp = new ClientPolicy() {
      eventLoops = new NioEventLoops(new EventPolicy(), 1)
    }

    private val client = new AerospikeClient(cp, host, port)

    override def exec[R](func: (AerospikeClient, EventLoop) => R): R =
      func(client, cp.eventLoops.next)

    override def close(): Unit =
      client.close()
  }
}

trait AeroClient extends AeroContext with AutoCloseable
