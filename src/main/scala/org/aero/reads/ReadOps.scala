package org.aero.reads

import com.aerospike.client.listener.{ExistsListener, RecordListener, RecordSequenceListener}
import com.aerospike.client.{AerospikeException, Key, Record, Value}
import org.aero.common.KeyBuilder._
import org.aero.common.{KeyDecoder, KeyEncoder}
import org.aero.reads.ReadOps.BinSchemaMagnet
import org.aero.{AeroContext, Schema}
import shapeless._
import shapeless.ops.hlist._
import shapeless.ops.record._
import shapeless.tag.Tagged

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

trait TypeMagnet {
  type Out
}

trait Converter[T] {
  def decode(m: Record): T
  def binNames: List[String]
}

trait Encoder {
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
  ): Encoder = {
    new Encoder {
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

trait ReadOps {

  def batchGet[K](keys: Seq[K], encoder: Encoder)(implicit aec: AeroContext,
                                                  enc: KeyEncoder[K],
                                                  dec: KeyDecoder[K],
                                                  schema: Schema): Future[Seq[(K, encoder.Out)]] = {

    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.batchPolicyDefault
      //  TODO FIX default defaultPolicy.maxConcurrentThreads = 1 - sequentially

      val promise = Promise[Seq[(K, encoder.Out)]]

      def marshaller(key: Value, record: Record) = {
        dec.decode(key) -> encoder.converter.decode(record)
      }

      val listener: RecordSequenceListener = Listeners.mkBatch(promise, keys.length, (k, v) => marshaller(k, v))

      try {
        ac.get(loop, listener, defaultPolicy, keys.map(k => make(k)).toArray, encoder.converter.binNames: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
  }

  def getAs[K](key: K, encoder: Encoder)(implicit aec: AeroContext,
                                         kw: KeyEncoder[K],
                                         schema: Schema): Future[Option[encoder.Out]] = {

    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise       = Promise[Option[encoder.Out]]()

      def onSuccess(recordOpt: Option[Record]): Unit =
        promise.complete(Try(recordOpt.map(rec => encoder.converter.decode(rec))))

      val listener = Listeners.recordOptListener(onSuccess, promise.failure(_))
      try {
        ac.get(loop, listener, defaultPolicy, make(key), encoder.converter.binNames: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }
  }

  def get[K](key: K, magnet: BinSchemaMagnet)(implicit aec: AeroContext,
                                              kw: KeyEncoder[K],
                                              schema: Schema): Future[Option[magnet.Out]] =
    aec.exec { (ac, loop) =>
      val defaultPolicy = ac.readPolicyDefault
      val promise       = Promise[Option[magnet.Out]]()

      def onSuccess(recordOpt: Option[Record]): Unit =
        promise.complete(Try(recordOpt.map(rec => magnet.decode(rec))))

      val listener = Listeners.recordOptListener(onSuccess, promise.failure(_))

      try {
        ac.get(loop, listener, defaultPolicy, make(key), magnet.keys: _*)
      } catch {
        case NonFatal(e) =>
          promise.failure(e)
      }
      promise.future
    }

  def exists[K](key: K)(implicit aec: AeroContext, kw: KeyEncoder[K], schema: Schema): Future[Boolean] = {
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

object Listeners {
  def recordOptListener(success: Option[Record] => Unit, failure: Exception => Unit): RecordListener =
    new RecordListener {
      override def onSuccess(key: Key, record: Record): Unit = {
        success(Option(record))
      }
      override def onFailure(exception: AerospikeException): Unit =
        failure(exception)
    }

  def mkBatch[K, V](promise: Promise[Seq[(K, V)]], size: Int, encoder: (Value, Record) => (K, V)) =
    new RecordSequenceListener {
      val builder = ArrayBuffer.newBuilder[(K, V)]
      builder.sizeHint(size)

      override def onRecord(key: Key, record: Record): Unit = {
        if(record == null)
          println(s"Key: ${key.userKey} not found")
        Option(record).map(v => encoder(key.userKey, v)).foreach { tuple =>
          builder += tuple
        }
      }

      override def onSuccess(): Unit = {
        promise.success(builder.result().toVector)
      }

      override def onFailure(exception: AerospikeException): Unit =
        promise.failure(exception)
    }
}
