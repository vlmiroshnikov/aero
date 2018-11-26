package org.aero.writes

import com.aerospike.client.Value

trait PartialEncoder[T] {
  def encode(v: T): Value
}
