package org.aero.reads

case class KeyNotFoundException(key: String) extends RuntimeException(s"Key `$key` not found")
