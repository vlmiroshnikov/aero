package org.aero.impl.reads.streams

import akka.Done
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.aerospike.client.listener.RecordSequenceListener
import com.aerospike.client.{AerospikeException, Key, Record}
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

class SourceStage[T](bufferSize: Int, build: (Key, Record) => T, action: RecordSequenceListener => Unit)
    extends GraphStageWithMaterializedValue[SourceShape[T], Future[Done]] {

  private val out: Outlet[T] = Outlet("source")

  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val promise = Promise[Done]

    (new GraphStageLogic(shape) {
      private val queue = scala.collection.mutable.Queue[T]()

      private val onFailureCallback = getAsyncCallback[Throwable] { ex =>
        failStage(ex)
      }

      private val listener = new RecordSequenceListener {
        override def onFailure(exception: AerospikeException): Unit =
          onFailureCallback.invoke(exception)
        override def onRecord(key: Key, record: Record): Unit =
          onMessage.invoke(build(key, record))

        override def onSuccess(): Unit = {}
      }

      private val onMessage = getAsyncCallback[T] { message =>
        require(queue.size <= bufferSize)
        if (isAvailable(out)) {
          pushMessage(message)
        } else {
          queue.enqueue(message)
        }
      }

      override def preStart(): Unit =
        try {
          action(listener)
        } catch {
          case NonFatal(e) =>
            failStage(e)
        }

      setHandler(out, new OutHandler {
        override def onPull(): Unit =
          if (queue.nonEmpty) {
            pushMessage(queue.dequeue())
          }
      })

      def pushMessage(message: T): Unit =
        push(out, message)

    }, promise.future)
  }
}
