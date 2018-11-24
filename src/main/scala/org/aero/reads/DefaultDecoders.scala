package org.aero.reads

import com.aerospike.client.Record
import scala.collection.JavaConverters._

trait DefaultDecoders {
  implicit val decoderBoolean: PartialDecoder[Boolean] = (a: Record, key: String) => { a.getBoolean(key) }
  implicit val decoderInt: PartialDecoder[Int]         = (a: Record, key: String) => { a.getInt(key) }
  implicit val decoderString: PartialDecoder[String]   = (a: Record, key: String) => { a.getString(key) }
  implicit val decoderDouble: PartialDecoder[Double]   = (a: Record, key: String) => { a.getDouble(key) }

  implicit def decoderOption[T](implicit decoder: PartialDecoder[T]): PartialDecoder[Option[T]] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(decoder.decode(a, key)) else None
  }

  implicit def decoderList[T]: PartialDecoder[List[T]] = (a: Record, key: String) => {
    a.getList(key).asScala.toList.asInstanceOf[List[T]]
  }

  implicit def decoderMap[K, V]: PartialDecoder[Map[K, V]] = (a: Record, key: String) => {
    a.getMap(key).asScala.toMap.asInstanceOf[Map[K, V]]
  }
}
