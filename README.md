Aero [![travis-badge][]][travis]
====

[travis]:                https://travis-ci.org/vlmiroshnikov/aero
[travis-badge]:          https://travis-ci.org/vlmiroshnikov/aero.svg?branch=master

This project provides scala client to Aerospike database.

Dependencies 
------------
- scala 2.12
- shapeless-2.3.3
- java aerospike-client 4


Quick start 
-----------
To start working with Aero you have to add dependency sbt:
```scala
  libraryDependencies += "org.aero" % "aero" % "0.2.1" 
```

Now you can use it like this:

```scala
    import scala.concurrent.duration._
    import org.aero.AeroOps._
    import org.aero._
    
    import scala.concurrent.Await
    import scala.concurrent.ExecutionContext.Implicits.global
    
    object Main {
      def main(args: Array[String]): Unit = {
        val host = "localhost"
        val port = 3000
    
        implicit val schema = Schema("test", "org-os")
        implicit val ac = AeroClient(host, port)
    
        val eventualUnit = for {
          _ <- put("002", (WriteBin("key", "002"), WriteBin("string_column", "string"), WriteBin("double_column", 1.1)))
          (key, value) <- get("002", ("key".as[String], "double_column".as[Double]))
          optionResult <- getOpt("002", "string_column".as[String])
        } yield {
          println(s"$key = $value") //   002 = 1.1
          println(optionResult)     //   Some(String)
        }
    
        Await.ready(eventualUnit, Duration.Inf)
    
        ac.close()
      }
    }
```
