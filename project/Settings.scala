import sbt._

object Settings {

  object Versions {
    val scala     = "2.12.8"
    val shapless  = "2.3.3"
    val aerospike = "4.4.9"
    val effect    = "1.2.0"
    val cats      = "1.6.0"
    val monix     = "3.0.0-RC2"
  }

  val scalacOptions = Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-language:higherKinds",
    "-Ypartial-unification",
    "-Ywarn-unused"
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
