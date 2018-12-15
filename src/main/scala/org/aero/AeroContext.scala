package org.aero

import com.aerospike.client.AerospikeClient
import com.aerospike.client.async.EventLoop
import org.aero.AeroContext.Callback

import scala.language.higherKinds

trait AeroContext[F[_]] {
  def exec[R](c: (AerospikeClient, EventLoop, Callback[R]) => Unit): F[R]
}

object AeroContext {
  type Callback[-A] = Either[Throwable, A] => Unit
}
