package org.aero

import org.aero.common.{DefaultKeyDecoders, DefaultKeyEncoders}
import org.aero.writes._
import org.aero.reads._

import scala.language.higherKinds

trait AeroOps[F[_]]
    extends WriteOps[F]
    with ReadOps[F]
    with ToNames
    with DefaultDecoders
    with DefaultEncoders
    with DefaultKeyEncoders
    with DefaultKeyDecoders
    with ValueBinOps
    with TypeMagnetOps
