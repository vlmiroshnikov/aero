package org.aero.writes

case class ValueBin[T](name: String, value: T)

trait ValueBinOps {
  implicit class valueBinCtor(name: String) {
    def ->>[T](value: T): ValueBin[T] = ValueBin(name, value)
  }
}
