package org.aero.reads

import com.aerospike.client.listener.{ExistsListener, RecordListener}
import com.aerospike.client.{AerospikeException, Key, Record}
import org.aero.common.KeyWrapper
import org.aero.reads.ReadOps.BinSchemaMagnet
import org.aero.{AeroContext, Schema}
import shapeless.ops.hlist._
import shapeless._

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Success, Try}
import org.aero.common.KeyBuilder._

trait ReadOps {

  def get[K, V <: Product](key: K)(implicit aec: AeroContext, kw: KeyWrapper[K], schema: Schema): Future[V] = {}

  def get[K](key: K, magnet: BinSchemaMagnet)(implicit aec: AeroContext,
                                              kw: KeyWrapper[K],
                                              schema: Schema): Future[magnet.Out] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise       = Promise[magnet.Out]()

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
        ac.get(loop, listener, defaultPolicy, make(key), magnet.names: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }

  def getOpt[K](key: K, magnet: BinSchemaMagnet)(implicit aec: AeroContext,
                                                 kw: KeyWrapper[K],
                                                 schema: Schema): Future[Option[magnet.Out]] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise       = Promise[Option[magnet.Out]]()

      val listener = new RecordListener {
        override def onFailure(exception: AerospikeException): Unit =
          promise.failure(exception)

        override def onSuccess(key: Key, record: Record): Unit =
          promise.complete(Try(Option(record) map { rec =>
            magnet.extract(rec)
          }))
      }

      try {
        ac.get(loop, listener, defaultPolicy, make(key), magnet.names: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }

  def exists[K](key: K)(implicit aec: AeroContext, kw: KeyWrapper[K], schema: Schema): Future[Boolean] = {
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise       = Promise[Boolean]()

      val listener = new ExistsListener {
        override def onFailure(exception: AerospikeException): Unit = {
          promise.failure(exception)
        }

        override def onSuccess(key: Key, exists: Boolean): Unit = {
          promise.complete(Success(exists))
        }
      }

      try {
        ac.exists(loop, listener, defaultPolicy, make(key))
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
  }
}

object ReadOps {

  trait BinSchemaMagnet {
    type Out

    def names: Seq[String]
    def extract(in: Record): Out
  }

  object BinSchemaMagnet {
    implicit def apply[T](name: T)(implicit pdef: ParamDef[T]): BinSchemaMagnet { type Out = pdef.Out } =
      new BinSchemaMagnet {
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

        def names(a: A): Seq[String]    = gn(a)
        def apply(r: Record, a: A): Out = f(a)(r)
      }

    private def extractParameter[A, B](f: A => Record => B, gn: A => Seq[String]): ParamDefAux[A, B] =
      paramDef(f, gn)

    private def extract[B](key: String)(implicit decoder: Decoder[B]): Record => B = { r =>
      decoder.decode(r, key)
    }

    implicit def forNamed[T](implicit decoder: Decoder[T]): ParamDefAux[Named[T], T] =
      extractParameter[Named[T], T](nr => extract(nr.name), nr => Seq(nr.name))

    implicit def forNamedOption[T](implicit decoder: Decoder[Option[T]]): ParamDefAux[NamedOption[T], Option[T]] =
      extractParameter[NamedOption[T], Option[T]](nr => extract(nr.name), nr => Seq(nr.name))

    implicit def forTuple[T <: Product, L <: HList, M <: HList, S <: HList, Out](
        implicit
        genFrom: Generic.Aux[T, L],
        mapper: Mapper.Aux[magnetize.type, L, M],
        mm: HListTransformer.Aux[M, S],
        tupler: Tupler.Aux[S, Out],
        travers: ToTraversable.Aux[M, List, BinSchemaMagnet]
    ): ParamDefAux[T, tupler.Out] =
      paramDef[T, tupler.Out](
        params => {
          val mat = HListMaterializer(genFrom.to(params).map(magnetize))
          record =>
            mat.map(record).tupled
        },
        params => genFrom.to(params).map(magnetize).toList[BinSchemaMagnet].flatMap(_.names)
      )


    object magnetize extends Poly1 {
      implicit def named[M](implicit pd: ParamDef[Named[M]]) =
        at[Named[M]](nr => BinSchemaMagnet.apply(nr)(pd))

      implicit def namedOption[M](implicit pd: ParamDef[NamedOption[M]]) =
        at[NamedOption[M]](nr => BinSchemaMagnet.apply(nr)(pd))
    }
  }
}
