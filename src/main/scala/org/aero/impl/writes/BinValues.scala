package org.aero.impl.writes

import com.aerospike.client.Value._
import com.aerospike.client._
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.Mapper
import shapeless.ops.record.ToMap
import shapeless.record._
import shapeless.{HList, Poly1}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

object mapper extends Poly1 {
  implicit def caseInt[K] =
    at[FieldType[K, Int]](i => field[K](Value.get(i)))

  implicit def caseLong[K] =
    at[FieldType[K, Long]](i => field[K](Value.get(i)))

  implicit def caseString[K] =
    at[FieldType[K, String]](i => field[K](Value.get(i)))

  implicit def caseList[K] =
    at[FieldType[K, List[String]]](i => field[K](new ListValue(i.asJava)))

  implicit def caseDouble[K] =
    at[FieldType[K, Double]](i => field[K](new DoubleValue(i)))
}

class BinValues private (items: List[Bin], ttlValue: Option[FiniteDuration]) {
  def values: List[Bin] = items
  def ttl: Option[FiniteDuration] = ttlValue
}

object BinValues {
  def apply[L <: HList, M <: HList](
      list: L,
      ttl: Option[FiniteDuration] = None
  )(implicit mapp: Mapper.Aux[mapper.type, L, M], toMap: ToMap.Aux[M, String, Value]): BinValues = {

    val bins =
      list.map(mapper).toMap.map { case (k, v) => new Bin(k.toString, v) }
    new BinValues(bins.toList, ttl)
  }
}
