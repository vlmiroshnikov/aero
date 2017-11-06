package org.aero

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.aero.AeroOps._
import org.aero.AeroStreamedOps._
import org.aero.impl.writes.BinValues
import shapeless._
import shapeless.syntax.singleton._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.{higherKinds, implicitConversions}

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 3000

    implicit val schema = Schema("test", "org-os")
    implicit val ac = AeroClient(host, port)

    implicit val as = ActorSystem("default")
    implicit val mat = ActorMaterializer()
    implicit val ec = as.dispatcher

    val r = put("001", BinValues(("js" ->> "data") :: ("key" ->> 1) :: HNil))
    Await.ready(r, Duration.Inf)

    val res = get("000", ("js".as[String], "key".as[Int]))
    val f = getBatch(Seq("000", "001"), ("js".as[String], "key".as[Int])).to(Sink.foreach(r => println(r))).run()
    Await.ready(f, Duration.Inf)
    //val m = Await.result(res, Duration.Inf)
    //println(m)
    ac.close()
  }
}
