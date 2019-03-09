package org.aero.reads

import com.aerospike.client.listener.{ExistsListener, RecordSequenceListener}
import com.aerospike.client.query.Statement
import com.aerospike.client.{AerospikeException, Key, Record, Value}
import org.aero.common.KeyBuilder._
import org.aero.common.{KeyDecoder, KeyEncoder, Listeners}
import org.aero.reads.ReadOps.BinSchemaMagnet
import org.aero.{AeroContext, Schema}
import shapeless._
import shapeless.ops.hlist._
import shapeless.ops.record._
import shapeless.tag.Tagged

import scala.util.Try
import scala.util.control.NonFatal
import scala.language.implicitConversions

trait TypeMagnet {
  type Out
}

trait Converter[T] {
  def decode(m: Record): T
  def binNames: List[String]
}

trait EncoderMagnet {
  type Out
  def converter: Converter[Out]
}

object as {
  def apply[T <: Product]: TypeMagnet.Aux[T] = new TypeMagnet {
    type Out = T
  }
}

object TypeMagnet {
  type Aux[Repr] = TypeMagnet { type Out = Repr }
}

trait TypeMagnetOps {
  private object symbolName extends Poly1 {
    implicit def atTaggedSymbol[T]: Case[Symbol with Tagged[T]] {
      type Result = String
    } = at[Symbol with Tagged[T]](_.name)
  }

  implicit def toConverter[T, R <: HList, L <: HList, Z <: HList](tp: TypeMagnet.Aux[T])(
      implicit gen: LabelledGeneric.Aux[T, R],
      fromMap: FromRecord[R],
      keysFrom: Keys.Aux[R, L],
      mapper: Mapper.Aux[symbolName.type, L, Z],
      traversable: ToTraversable.Aux[Z, List, String],
  ): EncoderMagnet = {
    new EncoderMagnet {
      type Out = tp.Out
      def converter: Converter[Out] = new Converter[Out] {
        def decode(m: Record): Out = {
          fromMap(m).map(gen.from).getOrElse(throw new Exception("Encoding failure"))
        }
        override def binNames: List[String] = {
          traversable.apply(keysFrom().map(symbolName))
        }
      }
    }
  }
}

trait ReadOps[F[_]] {

  def query(statement: Statement, magnet: EncoderMagnet)(implicit aec: AeroContext[F]): F[Seq[magnet.Out]] = {
    aec.exec { (ac, loop, cb) =>
      def marshaller(record: Record) = Try {
        magnet.converter.decode(record)
      }

      val listener: RecordSequenceListener = Listeners.mkSeq(cb, 1000, v => marshaller(v))

      try {
        val defaultPolicy = ac.queryPolicyDefault
        ac.query(loop, listener, defaultPolicy, statement)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }

  def batchGetAs[K](
      keys: Seq[K],
      magnet: EncoderMagnet
  )(implicit aec: AeroContext[F], enc: KeyEncoder[K], dec: KeyDecoder[K], schema: Schema): F[Seq[(K, Option[magnet.Out])]] = {

    aec.exec { (ac, loop, cb) =>
      def marshaller(key: Value, recordOpt: Option[Record]) = Try {
        dec.decode(key) -> recordOpt.map(record => magnet.converter.decode(record))
      }

      val listener: RecordSequenceListener = Listeners.mkBatch(cb, keys.length, (k, v) => marshaller(k, v))

      try {
        val defaultPolicy = ac.batchPolicyDefault
        ac.get(loop, listener, defaultPolicy, keys.map(k => make(k)).toArray, magnet.converter.binNames: _*)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }

  def getAs[K](
      key: K,
      magnet: EncoderMagnet
  )(implicit aec: AeroContext[F], kw: KeyEncoder[K], schema: Schema): F[Option[magnet.Out]] = {

    aec.exec { (ac, loop, cb) =>
      def marshaller(record: Record) =
        Try(magnet.converter.decode(record))

      val listener = Listeners.recordOptListener(cb, rec => marshaller(rec))
      try {
        val defaultPolicy = ac.readPolicyDefault
        ac.get(loop, listener, defaultPolicy, make(key), magnet.converter.binNames: _*)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }

  def get[K](
      key: K,
      magnet: BinSchemaMagnet
  )(implicit aec: AeroContext[F], kw: KeyEncoder[K], schema: Schema): F[Option[magnet.Out]] =
    aec.exec { (ac, loop, cb) =>
      val defaultPolicy = ac.readPolicyDefault

      def marshaller(record: Record) =
        Try(magnet.decode(record))

      val listener = Listeners.recordOptListener(cb, rec => marshaller(rec))

      try {
        ac.get(loop, listener, defaultPolicy, make(key), magnet.keys: _*)
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }

  def exists[K](
      key: K
  )(implicit aec: AeroContext[F], kw: KeyEncoder[K], schema: Schema): F[Boolean] = {
    aec.exec { (ac, loop, cb) =>
      val defaultPolicy = ac.readPolicyDefault

      val listener = new ExistsListener {
        override def onFailure(exception: AerospikeException): Unit = {
          cb(Left(exception))
        }

        override def onSuccess(key: Key, exists: Boolean): Unit = {
          cb(Right(exists))
        }
      }

      try {
        ac.exists(loop, listener, defaultPolicy, make(key))
      } catch {
        case NonFatal(e) =>
          cb(Left(e))
      }
    }
  }
}

object ReadOps {

  trait BinSchemaMagnet {
    type Out

    def keys: Seq[String]
    def decode(in: Record): Out
  }

  object BinSchemaMagnet {
    implicit def apply[T](obj: T)(implicit pdef: ParamDef[T]): BinSchemaMagnet { type Out = pdef.Out } =
      new BinSchemaMagnet {
        type Out = pdef.Out

        override def keys: Seq[String] = pdef.keys(obj)
        override def decode(rec: Record): Out =
          pdef.apply(rec, obj)
      }
  }

  type ParamDefAux[T, U] = ParamDef[T] { type Out = U }

  sealed trait ParamDef[T] {
    type Out

    def keys(key: T): Seq[String]
    def apply(r: Record, key: T): Out
  }

  object ParamDef {
    def paramDef[A, B](f: A => Record => B, gn: A => Seq[String]): ParamDefAux[A, B] =
      new ParamDef[A] {
        type Out = B

        def keys(a: A): Seq[String]     = gn(a)
        def apply(r: Record, a: A): Out = f(a)(r)
      }

    private def extractParameter[A, B](f: A => Record => B, gn: A => Seq[String]): ParamDefAux[A, B] =
      paramDef(f, gn)

    private def extract[B](key: String)(implicit decoder: PartialDecoder[B]): Record => B = { r =>
      decoder.decode(r, key)
    }

    implicit def forNamed[T](implicit decoder: PartialDecoder[T]): ParamDefAux[Named[T], T] =
      extractParameter[Named[T], T](nr => extract(nr.name), nr => Seq(nr.name))

    implicit def forNamedOption[T](
        implicit decoder: PartialDecoder[Option[T]]
    ): ParamDefAux[NamedOption[T], Option[T]] =
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
        params => genFrom.to(params).map(magnetize).toList[BinSchemaMagnet].flatMap(_.keys)
      )

    object magnetize extends Poly1 {
      implicit def named[M](implicit pd: ParamDef[Named[M]]) =
        at[Named[M]](nr => BinSchemaMagnet.apply(nr)(pd))

      implicit def namedOption[M](implicit pd: ParamDef[NamedOption[M]]) =
        at[NamedOption[M]](nr => BinSchemaMagnet.apply(nr)(pd))
    }
  }
}
