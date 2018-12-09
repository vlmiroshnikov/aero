package org.aero.writes

import java.time.Instant
import java.util.Calendar

import com.aerospike.client.listener.DeleteListener
import com.aerospike.client.policy.{CommitLevel, WritePolicy}
import com.aerospike.client.{AerospikeException, Bin, Key}
import org.aero.common.KeyEncoder
import org.aero.common.KeyBuilder.make
import org.aero.writes.WriteOps.BinValueMagnet
import org.aero.{AeroContext, Schema}
import shapeless.ops.hlist
import shapeless.ops.record.Fields
import shapeless.{Generic, HList, LabelledGeneric, Poly1, Poly2}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Success
import scala.util.control.NonFatal

trait WriteOps {
  def append[K](key: K, magnet: BinValueMagnet, ttl: Option[FiniteDuration] = None)(implicit aec: AeroContext,
                                                                                    kw: KeyEncoder[K],
                                                                                    schema: Schema): Future[Unit] = {

    val promise = Promise[Unit]()
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.writePolicyDefault

      val policy = ttl.map { ttl =>
        val modified = new WritePolicy(defaultPolicy)
        modified.expiration = ttl.toSeconds.toInt
        modified
      } getOrElse defaultPolicy

      try {
        val bins = magnet().asInstanceOf[Seq[Bin]]
        ac.append(loop, Listeners.writeInstance(promise), policy, make(key), bins: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
    }
    promise.future
  }

  def put[K](key: K, magnet: BinValueMagnet, ttl: Option[FiniteDuration] = None)(implicit aec: AeroContext,
                                                                                 kw: KeyEncoder[K],
                                                                                 schema: Schema): Future[Unit] = {
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.writePolicyDefault
      defaultPolicy.sendKey = true

      val policy = ttl.map { ttl =>
        val modified = new WritePolicy(defaultPolicy)
        modified.expiration = ttl.toSeconds.toInt
        modified
      } getOrElse defaultPolicy

      val promise = Promise[Unit]()

      try {
        val bins = magnet().asInstanceOf[Seq[Bin]]
        ac.put(loop, Listeners.writeInstance(promise), policy, make(key), bins: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
  }

  def truncate(beforeLastUpdate: Instant)(implicit aec: AeroContext, schema: Schema): Unit = {
    aec.exec { (ac, _) =>
      val calendar = Calendar.getInstance()
      calendar.setTimeInMillis(beforeLastUpdate.toEpochMilli)
      ac.truncate(ac.infoPolicyDefault, schema.namespace, schema.set, calendar)
    }
  }

  def delete[K](key: K)(implicit aec: AeroContext, kw: KeyEncoder[K], schema: Schema): Future[Boolean] = {
    aec.exec { (ac, loop) =>
      val promise = Promise[Boolean]()
      val listener = new DeleteListener {
        override def onFailure(exception: AerospikeException): Unit = {
          promise.failure(exception)
        }

        override def onSuccess(key: Key, existed: Boolean): Unit = {
          promise.complete(Success(existed))
        }
      }

      try {
        ac.delete(loop, listener, ac.writePolicyDefault, make(key))
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }

      promise.future
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

  object WriteParamDef {

    def writeParamDef[A, B](f: A => B): WriteParamDefAux[A, B] =
      new WriteParamDef[A] {
        type Out = B
        override def apply(p: A): Out = f(p)
      }

    implicit def forWBin[T](implicit enc: PartialEncoder[T]): WriteParamDefAux[ValueBin[T], List[Bin]] =
      writeParamDef(a => List(new Bin(a.name, enc.encode(a.value))))

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
