package org.aero.impl.reads

import com.aerospike.client.Record
import shapeless.HList

private[reads] case class HListMaterializer[L <: HList](l: L) {
  def map[O <: HList](i: Record)(implicit ev: FactoryMap.Aux[L, O]): O =
    ev.transform(i, l)
}
