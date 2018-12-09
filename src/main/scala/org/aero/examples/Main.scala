package org.aero.examples

import com.aerospike.client.{Record, Value}
import org.aero.AeroOps._
import org.aero._
import org.aero.reads.{as, PartialDecoder}
import org.aero.writes.PartialEncoder

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 3000

    implicit val schema: Schema = Schema("bar", "sample6")
    implicit val ac: AeroClient = AeroClient(List(host), port)

    implicit val encoder1: PartialEncoder[BigDecimal] = (v: BigDecimal) => Value.get(v.doubleValue())

    implicit val decoder: PartialDecoder[BigDecimal] =
      (a: Record, key: String) => {
        a.getDouble(key)
      }

    case class Data(name: String)

    val insert = (0 to 5000).map(i => i -> Data(s"name ${i}")).map { case (i, d) => put(i, d) }
    Await.ready(Future.sequence(insert), Duration.Inf)

    val forAll = Seq.fill(1000)(Random.nextInt(5000))
//    val forSeq   = Seq.fill(300)(Random.nextInt(10000))


//    val seq   = Future.sequence(forAll.map(id => getAs(id, as[Data]))).onComplete(res => println(s"Seq completed ${res.get.flatten.length}"))
    val batch = batchGet(forAll, as[Data]).onComplete(res => println(s"Batch completed ${res.get.length}"))


    readChar()

//    val eventualUnit = for {
////      _ <- put("002", ("key" ->> "002", "string_column" ->> "string", "double_column" ->> 1.1))
//      _ <- put("002", dd)
////      _ <- put("003", ("key" ->> "003", "list_column" ->> List("string"), "map_column" ->> Map("key" -> "value")))
//
////      (key, value) <- get("002", ("key".as[String], "double_column".as[Double]))
////      optionResult <- getOpt("002", "string_column".as[String])
//    } yield ()

    //Await.ready(eventualUnit, Duration.Inf)
    ac.close()
  }
}
