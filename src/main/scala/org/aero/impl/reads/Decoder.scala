package org.aero.impl.reads

import com.aerospike.client.Record

trait Decoder[B] {
  def decode(a: Record, key: String): B
}
