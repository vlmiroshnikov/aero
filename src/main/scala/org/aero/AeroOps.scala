package org.aero

import org.aero.common.DefaultKeyWrappers
import org.aero.writes._
import org.aero.reads._

object AeroOps
    extends WriteOps
    with ReadOps
    with ToNames
    with DefaultDecoders
    with DefaultEncoders
    with DefaultKeyWrappers
    with ValueBinOps
