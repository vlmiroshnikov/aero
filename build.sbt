import Settings.Versions
import sbt.Keys._

lazy val aero = project
  .in(file("."))
  .settings(
    version       := "0.5.6",
    scalaVersion  := Versions.scala,
    scalacOptions := Settings.scalacOptions,
    libraryDependencies ++= Settings.dependencies,
    organization      := "org.aero",
    bintrayRepository := "aero",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomExtra :=
      <developers>
        <developer>
          <id>vlmiroshnikov</id>
          <name>Vyacheslav Miroshnikov</name>
        </developer>
      </developers>,
    bintrayPackageLabels := Seq("scala", "aerospike")
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(aero)
  .settings(
    version           := "0.1.0",
    scalaVersion      := Versions.scala,
    scalacOptions     := Settings.scalacOptions,
    organization      := "org.aero",
    bintrayRepository := "aero",
    libraryDependencies ++= Settings.dependencies ++ Settings.extraDependencies,
  )
