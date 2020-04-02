package org.aero.common
import com.aerospike.client.listener.{DeleteListener, RecordListener, RecordSequenceListener, WriteListener}
import com.aerospike.client.{AerospikeException, Key, Record, Value}
import org.aero.AeroContext.Callback

import scala.jdk.CollectionConverters._
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object Listeners {

  def deleteInstance(cb: Callback[Boolean]) = new DeleteListener {
    override def onFailure(exception: AerospikeException): Unit = {
      cb(Left(exception))
    }

    override def onSuccess(key: Key, existed: Boolean): Unit = {
      cb(Right(existed))
    }
  }

  def writeInstance(promise: Callback[Unit]): WriteListener = new WriteListener {
    override def onFailure(exception: AerospikeException): Unit =
      promise(Left(exception))
    override def onSuccess(key: Key): Unit = promise(Right(()))
  }

  def recordOptListener[V](promise: Callback[Option[V]], encoder: Record => Try[V]): RecordListener =
    new RecordListener {
      override def onSuccess(key: Key, record: Record): Unit = {
        Option(record).map(v => encoder(v).toEither).fold(promise(Right(Option.empty)))(v => promise(v.map(Option(_))))
      }
      override def onFailure(exception: AerospikeException): Unit =
        promise(Left(exception))
    }

  def mkBatch[K, V](promise: Callback[Seq[(K, Option[V])]],
                    sizeHint: Int,
                    encoder: (Value, Option[Record]) => Try[(K, Option[V])]): RecordSequenceListener =
    new RecordSequenceListener {
      val builder = ArrayBuffer.newBuilder[(K, Option[V])]
      builder.sizeHint(sizeHint)

      override def onRecord(key: Key, record: Record): Unit = {
        encoder(key.userKey, Option(record)).foreach { tuple => // TODO log Error ???
          builder += tuple
        }
      }

      override def onSuccess(): Unit = {
        promise(Right(builder.result().toVector))
      }

      override def onFailure(exception: AerospikeException): Unit =
        promise(Left(exception))
    }

  def mkSeq[V](promise: Callback[Seq[V]], sizeHint: Int, encoder: Record => Try[V]): RecordSequenceListener =
    new RecordSequenceListener {
      val builder = ArrayBuffer.newBuilder[V]
      builder.sizeHint(sizeHint)

      override def onRecord(key: Key, record: Record): Unit = {
        if (record == null)
          println(s"Key: ${key.userKey} not found")
        Option(record).flatMap(v => encoder(v).toOption).foreach { tuple => // TODO log Error ???
          builder += tuple
        }
      }

      override def onSuccess(): Unit = {
        promise(Right(builder.result().toVector))
      }

      override def onFailure(exception: AerospikeException): Unit =
        promise(Left(exception))
    }

  def mkCounterSeq(promise: Callback[Map[String, Long]]): RecordSequenceListener =
    new RecordSequenceListener {
      val counterMap = MMap.empty[String, Long]

      override def onRecord(key: Key, record: Record): Unit = {
        if (record != null) {
          record.bins.asScala.toList
            .foreach { case (bin, v) =>
              if (v != null) {
                val x = counterMap.getOrElse(bin, 0L)
                counterMap.update(bin, x + 1)
              }
            }
        } else {}
      }

      override def onSuccess(): Unit = {
        promise(Right(counterMap.toMap))
      }

      override def onFailure(exception: AerospikeException): Unit =
        promise(Left(exception))
    }
}
