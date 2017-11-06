package org

import com.aerospike.client.Value
import org.aero.impl.common.KeyWrapper

package object aero {
  implicit val stringKW: KeyWrapper[String] = KeyWrapper(Value.get)

  case class Schema(namespace: String, set: String)
}
