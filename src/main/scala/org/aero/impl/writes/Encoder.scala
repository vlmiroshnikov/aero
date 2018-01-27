package org.aero.impl.writes

import com.aerospike.client.Value

trait Encoder[T] {
  def encode(v: T): Value
}


