package org.aero.reads

import com.aerospike.client.Record

trait DefaultDecoders {
  implicit val stringDecoder: Decoder[String] = (a: Record, key: String) => { a.getString(key) }
  implicit val optionStringDecoder: Decoder[Option[String]] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(a.getString(key)) else None
  }

  implicit val intDecoder: Decoder[Int] = (a: Record, key: String) => { a.getInt(key) }
  implicit val optionIntDecoder: Decoder[Option[Int]] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(a.getInt(key)) else None
  }

  implicit val doubleDecoder: Decoder[Double] = (a: Record, key: String) => { a.getDouble(key) }
  implicit val optionDoubleDecoder: Decoder[Option[Double]] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(a.getDouble(key)) else None
  }

}
