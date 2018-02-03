package org.aero.writes

import com.aerospike.client.Value
import scala.collection.JavaConverters._

trait DefaultEncoders {
  implicit val encoderInt: Encoder[Int] = Value.get(_)
  implicit val encoderString: Encoder[String] = Value.get(_)
  implicit val encoderDouble: Encoder[Double] = Value.get(_)

  implicit def optionEncoder[T](implicit encoder: Encoder[T]): Encoder[Option[T]] = {
    case Some(r) => encoder.encode(r)
    case None => Value.getAsNull
  }

  implicit def listEncoder[T]: Encoder[List[T]] = (v: List[T]) => Value.get(v.asJava)
}
