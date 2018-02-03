package org.aero.reads

import com.aerospike.client.Record
import scala.collection.JavaConverters._

trait DefaultDecoders {
  implicit val stringDecoder: Decoder[String] = (a: Record, key: String) => { a.getString(key) }
  implicit val intDecoder: Decoder[Int] = (a: Record, key: String) => { a.getInt(key) }
  implicit val doubleDecoder: Decoder[Double] = (a: Record, key: String) => { a.getDouble(key) }

  implicit def optionDecoder[T](implicit decoder: Decoder[T]): Decoder[Option[T]] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(decoder.decode(a, key)) else None
  }

  implicit def listDecoder[T]: Decoder[List[T]] = (a: Record, key: String) => {
    a.getList(key).asScala.toList.asInstanceOf[List[T]]
  }
}
