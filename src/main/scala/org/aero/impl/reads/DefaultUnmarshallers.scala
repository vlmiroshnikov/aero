package org.aero.impl.reads

import com.aerospike.client.Record
import org.aero.impl.reads.ReadOps._

trait DefaultUnmarshallers {
  implicit val toS: FSU[String] = (a: Record, key: String) => {
    a.getString(key)
  }

  implicit val toSO: FSOU[String] = (a: Record, key: String) => {
    if (a.bins.containsKey(key)) Some(a.getString(key)) else None
  }

  implicit val toI: FSU[Int] = (a: Record, key: String) => {
    a.getInt(key)
  }
}
