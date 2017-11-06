package org.aero.impl.writes

import com.aerospike.client.listener.WriteListener
import com.aerospike.client.policy.WritePolicy
import com.aerospike.client.{AerospikeException, Key}
import org.aero.impl.common.KeyWrapper
import org.aero.{AeroContext, Schema}

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

trait WriteOps {
  def put[K](key: K, bins: => BinValues)(implicit aec: AeroContext, kw: KeyWrapper[K], schema: Schema): Future[Unit] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.writePolicyDefault

      val policy = bins.ttl.map { ttl =>
        val modified = new WritePolicy(defaultPolicy)
        modified.expiration = ttl.toSeconds.toInt
        modified
      } getOrElse defaultPolicy

      val promise = Promise[Unit]()

      val listener = new WriteListener {
        override def onFailure(exception: AerospikeException): Unit = promise.failure(exception)

        override def onSuccess(key: Key): Unit = promise.success(())
      }

      try {
        val k = new Key(schema.namespace, schema.set, kw.value(key))
        ac.put(loop, listener, policy, k, bins.values: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
}
