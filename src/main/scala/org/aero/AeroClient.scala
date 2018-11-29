package org.aero

import com.aerospike.client.{AerospikeClient, Host}
import com.aerospike.client.async._
import com.aerospike.client.policy.ClientPolicy

object AeroClient {
  def apply(hosts: List[String], port: Int): AeroClient = new AeroClient {
    private val cp = new ClientPolicy() {
      eventLoops = new NioEventLoops(new EventPolicy(), 1)
    }

    private val client = new AerospikeClient(cp, hosts.map(h => new Host(h, port)): _*)

    override def exec[R](func: (AerospikeClient, EventLoop) => R): R =
      func(client, cp.eventLoops.next)

    override def close(): Unit =
      client.close()
  }
}

trait AeroClient extends AeroContext with AutoCloseable
