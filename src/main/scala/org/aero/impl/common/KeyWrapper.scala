package org.aero.impl.common

import com.aerospike.client.Value

trait KeyWrapper[K] {
  def value(v: K): Value
}

object KeyWrapper {
  def apply[T](f: T => Value): KeyWrapper[T] = (v: T) => f(v)
}