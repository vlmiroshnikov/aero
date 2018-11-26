package org.aero.writes

import com.aerospike.client.Value
import scala.collection.JavaConverters._

trait DefaultEncoders {
  implicit val encoderInt: PartialEncoder[Boolean]   = Value.get(_)
  implicit val encoderBoolean: PartialEncoder[Int]   = Value.get(_)
  implicit val encoderString: PartialEncoder[String] = Value.get(_)
  implicit val encoderDouble: PartialEncoder[Double] = Value.get(_)

  implicit def optionEncoder[T](implicit encoder: PartialEncoder[T]): PartialEncoder[Option[T]] = {
    case Some(r) => encoder.encode(r)
    case None    => Value.getAsNull
  }

  implicit def encoderList[T]: PartialEncoder[List[T]]     = (v: List[T]) => Value.get(v.asJava)
  implicit def encoderMap[K, V]: PartialEncoder[Map[K, V]] = (v: Map[K, V]) => Value.get(v.asJava)
}
