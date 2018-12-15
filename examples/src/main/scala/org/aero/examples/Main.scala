package org.aero.examples

import cats.effect.{ExitCode, Resource}
import com.aerospike.client.{Record, Value}
import monix.eval.{Task, TaskApp}
import org.aero._
import org.aero.examples.TaskAeroOps._
import org.aero.reads.{as, PartialDecoder}
import org.aero.writes.PartialEncoder

import scala.util.Random

object Main extends TaskApp {
  def run(args: List[String]): Task[ExitCode] = {
    val host = "localhost"
    val port = 3000

    // Custom type encoders
    implicit val encoder: PartialEncoder[BigDecimal] = (v: BigDecimal) => Value.get(v.doubleValue())
    implicit val decoder: PartialDecoder[BigDecimal] =
      (a: Record, key: String) => a.getDouble(key)

    case class Data(name: String, value: BigDecimal)

    Resource
      .make(Task(AeroClient[Task](List(host), port)))(_.close())
      .use { implicit cl =>
        implicit val schema: Schema = Schema("dev", "test")

        val toInsert =
          (0 to 500).map(i => i -> Data(s"name $i", i)).map { case (i, d) => put(i, d) }

        for {
          _     <- Task.gatherUnordered(toInsert)
          _     <- delete(0)
          _     <- put(501, ("name" ->> "Name", "map" ->> Map("key1" -> 1, "key2" -> 2))) //  Bins : | name | map |
          tuple <- get(1, ("value".as[BigDecimal], "name".as[String]))
          data  <- getAs(1, as[Data]) // Or use case class

          items <- batchGetAs(Seq.fill(100)(Random.nextInt(500)), as[Data])
        } yield {
          println(s"At `1` ${tuple} = ${data}")
          println(s"Batch completed ${items.length}")
        }
      }
      .map(_ => ExitCode.Success)
  }
}
