package org.aero.impl.reads

import com.aerospike.client.Record
import org.aero.impl.reads.ReadOps.BinMagnet
import shapeless._
import scala.language.{higherKinds, implicitConversions}

private[reads] trait FactoryMap[L <: HList] {
  type Out <: HList
  def transform(i: Record, h: L): Out
}

private[reads] object FactoryMap {
  type Aux[L <: HList, O <: HList] = FactoryMap[L] { type Out = O }

  implicit def caseNil: Aux[HNil, HNil] = new FactoryMap[HNil] {
    type Out = HNil
    def transform(i: Record, l: HNil): HNil = l
  }

  implicit def caseCons[S, B <: BinMagnet, T <: HList, O <: HList](implicit ev: Aux[T, O]) = new FactoryMap[B :: T] {
    type Out = B#Out :: O
    def transform(i: Record, l: B :: T): B#Out :: O = {
      val (head :: tail) = l
      head.extract(i) :: ev.transform(i, tail)
    }
  }
}
