package org.aero.writes

import com.aerospike.client.listener.WriteListener
import com.aerospike.client.{AerospikeException, Key}

import scala.concurrent.Promise

private[writes] object Listeners {
  def writeInstance(promise: Promise[Unit]) = new WriteListener {
    override def onFailure(exception: AerospikeException): Unit =
      promise.failure(exception)
    override def onSuccess(key: Key): Unit = promise.success(())
  }
}
