package org.aero.writes

import com.aerospike.client.Value

trait Encoder[T] {
  def encode(v: T): Value
}


