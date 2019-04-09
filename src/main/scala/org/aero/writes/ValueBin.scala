package org.aero.writes

class ValueBin[T](val name: String, val value: T)

trait ValueBinOps {
  implicit class valueBinCtor(name: String) {
    def ->>[T](value: T): ValueBin[T] = {
      new ValueBin(name, value)
    }
  }
}
