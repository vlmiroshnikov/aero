import sbt._

object Settings {

  object Versions {
    val shapless = "2.3.3"
    val aerospike = "4.1.2"
  }

  val scalacOptions = Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-deprecation",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused"
  )

  val dependencies = Seq(
    "com.chuusai"   %% "shapeless"       % Versions.shapless,
    "com.aerospike" % "aerospike-client" % Versions.aerospike
  )
}



