package org.aero.reads
import com.aerospike.client.Record
import shapeless.HList
import shapeless._, labelled.{field, FieldType}

trait FromRecord[L <: HList] {
  def apply(m: Record): Either[Error, L]
}

object FromRecord {
  type Error = Exception
  implicit val hnilFromMap: FromRecord[HNil] = (_: Record) => Right(HNil)

  implicit def hconsFromMap[K <: Symbol, V, T <: HList](
      implicit
      witness: Witness.Aux[K],
      pd: PartialDecoder[V],
      fromMapT: Lazy[FromRecord[T]]
  ): FromRecord[FieldType[K, V] :: T] =
    (m: Record) =>
      for {
        v <- Right(pd.decode(m, witness.value.name))
        t <- fromMapT.value(m)
      } yield field[K](v) :: t
}
