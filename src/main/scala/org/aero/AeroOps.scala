package org.aero

import org.aero.common.{DefaultKeyDecoders, DefaultKeyEncoders}
import org.aero.writes._
import org.aero.reads._

object AeroOps
    extends WriteOps
    with ReadOps
    with ToNames
    with DefaultDecoders
    with DefaultEncoders
    with DefaultKeyEncoders
    with DefaultKeyDecoders
    with ValueBinOps
    with TypeMagnetOps
