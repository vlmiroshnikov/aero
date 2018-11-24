package org.aero.reads

import com.aerospike.client.Record
import org.aero.reads.ReadOps.BinSchemaMagnet
import shapeless._

private[reads] trait HListTransformer[L <: HList] {
  type Out <: HList
  def transform(i: Record, h: L): Out
}

private[reads] object HListTransformer {
  type Aux[L <: HList, O <: HList] = HListTransformer[L] { type Out = O }

  implicit def caseNil: Aux[HNil, HNil] = new HListTransformer[HNil] {
    type Out = HNil
    def transform(i: Record, l: HNil): HNil = l
  }

  implicit def caseCons[S, B <: BinSchemaMagnet, T <: HList, O <: HList](implicit ev: Aux[T, O]) =
    new HListTransformer[B :: T] {
      type Out = B#Out :: O
      def transform(i: Record, l: B :: T): B#Out :: O = {
        val head :: tail = l
        head.decode(i) :: ev.transform(i, tail)
      }
    }
}
