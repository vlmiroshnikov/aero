package org.aero.reads.streams

import akka.Done
import akka.stream.scaladsl.Source
import com.aerospike.client.Key
import org.aero.common.KeyWrapper
import org.aero.reads.ReadOps.BinMagnet
import org.aero.{AeroContext, Schema}

import scala.concurrent.Future

trait StreamedReadOps {
  def getBatch[K](keys: Seq[K], magnet: BinMagnet)(implicit aec: AeroContext,
                                                   kw: KeyWrapper[K],
                                                   schema: Schema): Source[magnet.Out, Future[Done]] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.batchPolicyDefault

      val stage = new SourceStage(
        keys.length,
        (_, record) => magnet.extract(record),
        listener => {
          val ks = keys.map(k => new Key(schema.namespace, schema.set, kw.value(k))).toArray
          ac.get(loop, listener, defaultPolicy, ks, magnet.names: _*)
        }
      )
      Source.fromGraph(stage)
    }
}
