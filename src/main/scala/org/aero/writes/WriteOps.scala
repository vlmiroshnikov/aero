package org.aero.writes

import java.time.Instant
import java.util.Calendar

import com.aerospike.client.listener.{DeleteListener, WriteListener}
import com.aerospike.client.policy.WritePolicy
import com.aerospike.client.{AerospikeException, Bin, Key}
import org.aero.common.KeyWrapper
import org.aero.writes.WriteOps.WBinMagnet
import org.aero.{AeroContext, Schema}
import shapeless.ops.hlist
import shapeless.{Generic, HList, Poly2}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Success
import scala.util.control.NonFatal

trait WriteOps {
  def put[K](key: K, magnet: WBinMagnet, ttl: Option[FiniteDuration] = None)(implicit aec: AeroContext,
                                                                             kw: KeyWrapper[K],
                                                                             schema: Schema): Future[Unit] = {
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.writePolicyDefault

      val policy = ttl.map { ttl =>
        val modified = new WritePolicy(defaultPolicy)
        modified.expiration = ttl.toSeconds.toInt
        modified
      } getOrElse defaultPolicy

      val promise = Promise[Unit]()

      val listener = new WriteListener {
        override def onFailure(exception: AerospikeException): Unit =
          promise.failure(exception)

        override def onSuccess(key: Key): Unit = promise.success(())
      }

      try {
        val bins = magnet().asInstanceOf[Seq[Bin]]
        val k = new Key(schema.namespace, schema.set, kw.value(key))
        ac.put(loop, listener, policy, k, bins: _*)
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

  def delete[K](key: K)(implicit aec: AeroContext, kw: KeyWrapper[K], schema: Schema): Future[Boolean] = {
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
        val k = new Key(schema.namespace, schema.set, kw.value(key))
        ac.delete(loop, listener, ac.writePolicyDefault, k)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }

      promise.future
    }
  }
}

object WriteOps {

  trait WBinMagnet {
    type Out

    def apply(): Out
  }

  object WBinMagnet {
    implicit def apply[T](value: T)(implicit wpd: WriteParamDef[T]) = new WBinMagnet {
      type Out = wpd.Out

      override def apply(): Out = wpd.apply(value)
    }
  }

  type WriteParamDefAux[T, U] = WriteParamDef[T] {type Out = U}

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

    implicit def forWBin[T](implicit enc: Encoder[T]): WriteParamDefAux[WBin[T], List[Bin]] =
      writeParamDef(a => List(new Bin(a.name, enc.encode(a.value))))

    implicit def fopTuple[T, L <: HList](
                                          implicit
                                          gen: Generic.Aux[T, L],
                                          folder: hlist.LeftFolder[L, List[Bin], Reducer.type]
                                        ): WriteParamDefAux[T, folder.Out] =
      writeParamDef { p =>
        gen.to(p).foldLeft(List.empty[Bin])(Reducer)
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
