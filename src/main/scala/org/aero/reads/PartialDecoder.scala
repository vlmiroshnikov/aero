package org.aero.reads

import com.aerospike.client.Record

trait PartialDecoder[B] {
  def decode(a: Record, key: String): B
}
