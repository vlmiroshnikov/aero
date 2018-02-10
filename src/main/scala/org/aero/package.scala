package org

import org.aero.writes.WBin

package object aero {
  type WriteBin[T] = WBin[T]
  val WriteBin = WBin

  case class Schema(namespace: String, set: String)
}
