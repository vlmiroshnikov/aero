package org.aero.impl.reads

case class KeyNotFoundException(key: String) extends RuntimeException(s"Key `$key` not found")
