package org.aero

import cats.effect.Async
import com.aerospike.client.{AerospikeClient, Host}
import com.aerospike.client.async._
import com.aerospike.client.policy.ClientPolicy
import org.aero.AeroContext.Callback

object AeroClient {
  def apply[F[_]](hosts: List[String],
                  port: Int,
                  maxConnectionsPerNode: Option[Int] = None,
                  maxCommandsInProcess: Option[Int] = None,
                  maxCommandsInQueue: Option[Int] = None,
                  commandsPerEventLoop: Option[Int] = None)
                 (implicit F: Async[F]): AeroClient[F] = new AeroClient[F] {
    private val eventPolicy = new EventPolicy()
    eventPolicy.maxCommandsInProcess = maxCommandsInProcess.getOrElse(0)
    eventPolicy.maxCommandsInQueue = maxCommandsInQueue.getOrElse(0)
    eventPolicy.commandsPerEventLoop = commandsPerEventLoop.getOrElse(256)
    private val cp = new ClientPolicy() {
      maxConnsPerNode = maxConnectionsPerNode.getOrElse(300)
      eventLoops      = new NioEventLoops(eventPolicy, -1)
    }

    private val client = new AerospikeClient(cp, hosts.map(h => new Host(h, port)): _*)

    override def exec[R](func: (AerospikeClient, EventLoop, Callback[R]) => Unit): F[R] = {
      F.async[R] { cb =>
        func(client, cp.eventLoops.next, cb)
      }
    }

    def close(): F[Unit] =
      F.delay(client.close())
  }
}

trait AeroClient[F[_]] extends AeroContext[F] {
  def close(): F[Unit]
}
