package org.aero.examples

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.aero.AeroOps._
import org.aero.AeroStreamedOps._
import org.aero._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 3000


    implicit val schema = Schema("test", "org-os")
    implicit val ac = AeroClient(host, port)

    implicit val as = ActorSystem("default")
    implicit val mat = ActorMaterializer()


    val r = put("002", (WriteBin("key", "002"), WriteBin("her", "sdfsdf"), WriteBin("aa", 1.1)))
    Await.ready(r, Duration.Inf)

    //println(Await.result(get("000", ("js".as[String], "key".as[Int])), Duration.Inf))

    println(Await.result(getOpt("001", ("js".as[String], "key".as[Int])), Duration.Inf))
    val f = getBatch(Seq("000", "001"), ("js".as[String], "key".as[Int])).to(Sink.foreach(r => println(r))).run()
    Await.ready(f, Duration.Inf)
    //val m = Await.result(res, Duration.Inf)
    //println(m)
    ac.close()
    sys.exit(0)
  }
}
