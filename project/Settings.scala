import sbt._

object Settings {

  object Versions {
    val scala212 = "2.12.11"
    val scala213 = "2.13.1"
    val supportedScalaVersions = List(scala212, scala213)

    val shapless  = "2.3.3"
    val aerospike = "4.4.9"
    val effect    = "2.1.2"
    val cats      = "2.1.0"
    val monix     = "3.1.0"
  }

  val commonScalacOptions = Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-language:higherKinds",
    "-Ywarn-unused"
  )

  val scalacOptions = commonScalacOptions ++ Seq(
    "-Yno-adapted-args",
    "-Ypartial-unification"
  )

  val dependencies = Seq(
    "com.chuusai"   %% "shapeless"       % Versions.shapless,
    "com.aerospike" % "aerospike-client" % Versions.aerospike,
    "org.typelevel" %% "cats-core"       % Versions.cats,
    "org.typelevel" %% "cats-effect"     % Versions.effect
  )

  val extraDependencies = Seq(
    "io.monix" %% "monix" % Versions.monix
  )
}
