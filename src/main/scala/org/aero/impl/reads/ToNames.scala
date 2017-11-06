package org.aero.impl.reads

class Named[T](val name: String) {
  def as[B] = new Named[B](name)
  def ? = new NamedOption[T](name)
}

class NamedOption[T](val name: String)


trait ToNames {
  implicit def _symbol2NR(symbol: Symbol): Named[String] = new Named[String](symbol.name)
  implicit def _string2NR(string: String): Named[String] = new Named[String](string)
}
