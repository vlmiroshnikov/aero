package org.aero.impl.reads

import com.aerospike.client.listener.RecordListener
import com.aerospike.client.{AerospikeException, Key, Record}
import org.aero.impl.common.KeyWrapper
import org.aero.impl.reads.ReadOps.BinMagnet
import org.aero.{AeroContext, Schema}
import shapeless.ops.hlist._
import shapeless.{Generic, HList, Poly1}

import scala.concurrent.{Future, Promise}
import scala.util.Try
import scala.util.control.NonFatal

trait ReadOps {
  def get[K](key: K,
             magnet: BinMagnet)(implicit aec: AeroContext, kw: KeyWrapper[K], schema: Schema): Future[magnet.Out] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise = Promise[magnet.Out]()

      val listener = new RecordListener {
        override def onFailure(exception: AerospikeException): Unit =
          promise.failure(exception)

        override def onSuccess(key: Key, record: Record): Unit =
          Option(record) match {
            case None =>
              promise.failure(KeyNotFoundException(s"${key.namespace}:${key.setName}:${key.userKey}"))
            case Some(rec) =>
              promise.complete(Try(magnet.extract(rec)))
          }
      }

      try {
        val k = new Key(schema.namespace, schema.set, kw.value(key))
        ac.get(loop, listener, defaultPolicy, k, magnet.names: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }

  def getOpt[K](key: K, magnet: BinMagnet)(implicit aec: AeroContext,
                                           kw: KeyWrapper[K],
                                           schema: Schema): Future[Option[magnet.Out]] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise = Promise[Option[magnet.Out]]()

      val listener = new RecordListener {
        override def onFailure(exception: AerospikeException): Unit =
          promise.failure(exception)

        override def onSuccess(key: Key, record: Record): Unit =
          promise.complete(Try(Option(record) map { rec =>
            magnet.extract(rec)
          }))
      }

      try {
        val k = new Key(schema.namespace, schema.set, kw.value(key))
        ac.get(loop, listener, defaultPolicy, k, magnet.names: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
}

object ReadOps {
  type FSU[T] = Decoder[T]
  type FSOU[T] = Decoder[Option[T]]

  trait BinMagnet {
    type Out

    def names: Seq[String]
    def extract(in: Record): Out
  }

  object BinMagnet {
    implicit def apply[K](name: K)(implicit pdef: ParamDef[K]): BinMagnet { type Out = pdef.Out } = new BinMagnet {
      type Out = pdef.Out

      override def names: Seq[String] = pdef.names(name)
      override def extract(rec: Record): Out =
        pdef.apply(rec, name)
    }
  }

  type ParamDefAux[T, U] = ParamDef[T] { type Out = U }

  sealed trait ParamDef[T] {
    type Out
    def names(key: T): Seq[String]
    def apply(r: Record, key: T): Out
  }

  object ParamDef {
    def paramDef[A, B](f: A => Record => B, gn: A => Seq[String]): ParamDefAux[A, B] =
      new ParamDef[A] {
        type Out = B
        def names(a: A): Seq[String] = gn(a)
        def apply(r: Record, a: A): Out = f(a)(r)
      }

    private def extractParameter[A, B](f: A => Record => B, gn: A => Seq[String]): ParamDefAux[A, B] =
      paramDef(f, gn)

    private def extract[B](key: String)(implicit fsu: Decoder[B]): Record => B = { r =>
      fsu.decode(r, key)
    }

    implicit def forNamed[T](implicit fsu: FSU[T]): ParamDefAux[Named[T], T] =
      extractParameter[Named[T], T](nr => extract(nr.name), nr => Seq(nr.name))

    implicit def forNamedOption[T](implicit fsu: FSOU[T]): ParamDefAux[NamedOption[T], Option[T]] =
      extractParameter[NamedOption[T], Option[T]](nr => extract(nr.name), nr => Seq(nr.name))

    implicit def forTuple[T, L <: HList, M <: HList, S <: HList, Out](
        implicit
        genFrom: Generic.Aux[T, L],
        mapper: Mapper.Aux[magnetize.type, L, M],
        mm: HListTransformer.Aux[M, S],
        tupler: Tupler.Aux[S, Out],
        travers: ToTraversable.Aux[M, List, BinMagnet]
    ): ParamDefAux[T, tupler.Out] =
      paramDef[T, tupler.Out](
        params => {
          val mat = HListMaterializer(genFrom.to(params).map(magnetize))
          record =>
            mat.map(record).tupled
        },
        params => genFrom.to(params).map(magnetize).toList[BinMagnet].flatMap(_.names)
      )

    object magnetize extends Poly1 {
      implicit def named[M](implicit pd: ParamDef[Named[M]]) =
        at[Named[M]](nr => BinMagnet.apply(nr)(pd))

      implicit def namedOption[M](implicit pd: ParamDef[NamedOption[M]]) =
        at[NamedOption[M]](nr => BinMagnet.apply(nr)(pd))
    }
  }
}
