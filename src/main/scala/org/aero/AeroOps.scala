package org.aero

import org.aero.impl.reads.streams.StreamedReadOps
import org.aero.impl.writes._
import org.aero.impl.reads._

object AeroOps extends WriteOps with ReadOps with ToNames with DefaultDecoders with DefaultEncoders

object AeroStreamedOps extends StreamedReadOps