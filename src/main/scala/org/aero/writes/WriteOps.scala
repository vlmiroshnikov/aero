package org.aero.writes

import com.aerospike.client.Bin
import com.aerospike.client.policy.{RecordExistsAction, WritePolicy}
import org.aero.common.KeyBuilder.make
import org.aero.common.{KeyEncoder, Listeners}
import org.aero.writes.WriteOps.BinValueMagnet
import org.aero.writes.WriteOps.WriteParamDef.writeParamDef
import org.aero.{AeroContext, Schema}
import shapeless.ops.hlist
import shapeless.ops.record.Fields
import shapeless.{Generic, HList, LabelledGeneric, Poly1, Poly2}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.language.implicitConversions

trait WriteOps[F[_]] {
  def append[K](key: K, magnet: BinValueMagnet, ttl: Option[FiniteDuration] = None)(implicit aec: AeroContext[F],
                                                                                    kw: KeyEncoder[K],
                                                                                    schema: Schema): F[Unit] = {

    aec.exec { (ac, loop, cb) =>
      val defaultPolicy = ac.writePolicyDefault

      try {
        val policy = ttl.map { ttl =>
          val modified = new WritePolicy(defaultPolicy)
          modified.expiration = ttl.toSeconds.toInt
          modified
        } getOrElse defaultPolicy

        val bins = magnet().asInstanceOf[Seq[Bin]]
        ac.append(loop, Listeners.writeInstance(cb), policy, make(key), bins: _*)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }

  def put[K](key: K, magnet: BinValueMagnet, ttl: Option[FiniteDuration] = None)(implicit aec: AeroContext[F],
                                                                                 kw: KeyEncoder[K],
                                                                                 schema: Schema): F[Unit] = aec.exec {
    (ac, loop, cb) =>
      val modified = new WritePolicy(ac.writePolicyDefault)
      modified.expiration         = ttl.map(_.toSeconds.toInt).getOrElse(0)
      modified.recordExistsAction = RecordExistsAction.REPLACE

      try {
        val bins = magnet().asInstanceOf[Seq[Bin]]
        ac.put(loop, Listeners.writeInstance(cb), modified, make(key), bins: _*)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
  }

  def delete[K](
      key: K
  )(implicit aec: AeroContext[F], keyEncoder: KeyEncoder[K], schema: Schema): F[Boolean] = {
    aec.exec { (ac, loop, cb) =>
      try {
        ac.delete(loop, Listeners.deleteInstance(cb), ac.writePolicyDefault, make(key))
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }
}

object WriteOps {

  trait BinValueMagnet {
    type Out
    def apply(): Out
  }

  object BinValueMagnet {
    implicit def apply[T](value: T)(implicit wpd: WriteParamDef[T]): BinValueMagnet = new BinValueMagnet {
      type Out = wpd.Out
      override def apply(): Out = wpd.apply(value)
    }
  }

  type WriteParamDefAux[T, U] = WriteParamDef[T] { type Out = U }

  sealed trait WriteParamDef[T] {
    type Out
    def apply(p: T): Out
  }

  trait LowLevel {
    implicit def forWBin[T](implicit enc: PartialEncoder[T]): WriteParamDefAux[ValueBin[T], List[Bin]] =
      writeParamDef(a => List(new Bin(a.name, enc.encode(a.value))))
  }

  object WriteParamDef extends LowLevel {

    def writeParamDef[A, B](f: A => B): WriteParamDefAux[A, B] =
      new WriteParamDef[A] {
        type Out = B
        override def apply(p: A): Out = f(p)
      }

    implicit def fopTuple[T <: Product, L <: HList, Out](
        implicit gen: Generic.Aux[T, L],
        folder: hlist.LeftFolder.Aux[L, List[Bin], Reducer.type, Out]
    ): WriteParamDefAux[T, folder.Out] =
      writeParamDef { p =>
        gen.to(p).foldLeft(List.empty[Bin])(Reducer)
      }

    implicit def forCaseClass[T, L <: HList, K <: HList, R <: HList, Z <: HList, Out](
        implicit gen: LabelledGeneric.Aux[T, L],
        fields: Fields.Aux[L, R],
        mapper: hlist.Mapper.Aux[keysToString.type, R, Z],
        folder: hlist.LeftFolder[Z, List[Bin], Reducer.type]
    ): WriteParamDefAux[T, folder.Out] = {
      writeParamDef { p =>
        fields(gen.to(p)).map(keysToString).foldLeft(List.empty[Bin])(Reducer)
      }
    }

    object keysToString extends Poly1 {
      implicit def toWBin[A, B] = at[(Symbol with A, B)] {
        case (k, v) => ValueBin[B](k.name, v)
      }
    }

    object Reducer extends Poly2 {
      implicit def from[T](implicit pdma: WriteParamDefAux[T, List[Bin]]) = {
        at[List[Bin], T] { (a, t) =>
          a ::: pdma(t)
        }
      }
    }
  }
}
