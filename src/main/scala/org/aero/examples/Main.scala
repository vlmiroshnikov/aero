package org.aero.examples

import com.aerospike.client.{Record, Value}
import org.aero.AeroOps._
import org.aero._
import org.aero.reads.{PartialDecoder, as}
import org.aero.writes.PartialEncoder

import scala.concurrent.Await
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 3000

    implicit val schema: Schema = Schema("test", "sample")
    implicit val ac: AeroClient = AeroClient(List(host), port)

    implicit val encoder1: PartialEncoder[BigDecimal] = (v: BigDecimal) => Value.get(v.doubleValue())

    implicit val decoder: PartialDecoder[BigDecimal] =
      (a: Record, key: String) => {
        a.getDouble(key)
      }

    case class Nested(aa: Int, bbb: String)
    case class Data(key: String, name: String)

    val data = Data("005", "aa")

    Await.ready(put("005", data), Duration.Inf)
    val value = getAs("005", as[Data])
    println(Await.result(value, Duration.Inf))


    println(Await.result(get("002", ("key".as[String], "double_column".as[Double])), Duration.Inf))

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
