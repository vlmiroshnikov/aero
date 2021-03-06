package org.aero.reads

import com.aerospike.client.Record
import shapeless.HList

private[reads] case class HListMaterializer[L <: HList](l: L) {
  def map[O <: HList](i: Record)(implicit ev: HListTransformer.Aux[L, O]): O =
    ev.transform(i, l)
}
