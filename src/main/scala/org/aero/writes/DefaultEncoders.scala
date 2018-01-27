package org.aero.writes

import com.aerospike.client.Value

trait DefaultEncoders {
  implicit val encoderInt: Encoder[Int] = Value.get(_)
  implicit val encoderString: Encoder[String] = Value.get(_)
  implicit val encoderDouble: Encoder[Double] = Value.get(_)
}
