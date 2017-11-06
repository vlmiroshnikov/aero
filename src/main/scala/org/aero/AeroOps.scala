package org.aero

import org.aero.impl.reads.{DefaultUnmarshallers, ReadOps, ToNames}
import org.aero.impl.reads.streams.StreamedReadOps
import org.aero.impl.writes.WriteOps

object AeroOps extends WriteOps with ReadOps with ToNames with DefaultUnmarshallers

object AeroStreamedOps extends StreamedReadOps