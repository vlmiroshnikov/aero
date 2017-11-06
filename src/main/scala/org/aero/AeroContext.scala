package org.aero

import com.aerospike.client.AerospikeClient
import com.aerospike.client.async.EventLoop

trait AeroContext {
  def exec[R](c: (AerospikeClient, EventLoop) => R): R
}
