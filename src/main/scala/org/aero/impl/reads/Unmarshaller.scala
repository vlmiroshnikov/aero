package org.aero.impl.reads

import com.aerospike.client.Record

trait Unmarshaller[B] {
  def apply(a: Record, key: String): B
}
