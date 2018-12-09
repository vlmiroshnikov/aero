package org.aero.common

import com.aerospike.client.Value

trait KeyEncoder[K] {
  def encoder(v: K): Value
}

object KeyEncoder {
  def apply[T](f: T => Value): KeyEncoder[T] = (v: T) => f(v)
}



trait DefaultKeyEncoders {
  implicit val stringKW: KeyEncoder[String] = KeyEncoder(Value.get)
  implicit val intKW: KeyEncoder[Int]       = KeyEncoder(Value.get)
  implicit val longKW: KeyEncoder[Long]     = KeyEncoder(Value.get)
}

trait KeyDecoder[K] {
  def decode(value: Value): K
}

object KeyDecoder {
  def apply[K](f: Value => K): KeyDecoder[K] = (v: Value) => f(v)
}

trait DefaultKeyDecoders {
  implicit val stringDecoder: KeyDecoder[String] = KeyDecoder(_.getObject.asInstanceOf[String])
  implicit val intDecoder: KeyDecoder[Int]       = KeyDecoder(_.getObject.asInstanceOf[java.lang.Integer])
  implicit val longDecoder: KeyDecoder[Long]     = KeyDecoder(_.getObject.asInstanceOf[java.lang.Long])
}
