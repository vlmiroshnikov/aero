package org.aero.common

import com.aerospike.client.Value

trait KeyWrapper[K] {
  def value(v: K): Value
}

object KeyWrapper {
  def apply[T](f: T => Value): KeyWrapper[T] = (v: T) => f(v)
}

trait DefaultKeyWrappers {
  implicit val stringKW: KeyWrapper[String] = KeyWrapper(Value.get)
  implicit val intKW: KeyWrapper[Int] = KeyWrapper(Value.get)
  implicit val longKW: KeyWrapper[Long] = KeyWrapper(Value.get)
}
