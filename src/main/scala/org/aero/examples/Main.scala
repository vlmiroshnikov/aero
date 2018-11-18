package org.aero.examples

import org.aero.AeroOps._
import org.aero._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 3000

    implicit val schema: Schema = Schema("test", "sample")
    implicit val ac: AeroClient = AeroClient(host, port)

    case class Data(key: String, name: String, sdamole: Int, parama: Double)

    val dd = Data("005", "aa", 11, 1200.12)

    get[Data]("005")
    Await.ready(put("005", dd), Duration.Inf)



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
