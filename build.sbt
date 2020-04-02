import Settings.Versions
import sbt.Keys._

lazy val aero = project
  .in(file("."))
  .settings(
    version       := "0.6.0",
    scalaVersion  := Versions.scala212,
    scalacOptions ++=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          Settings.commonScalacOptions ++ Seq("-Xsource:2.14")
        case _ =>
          Settings.scalacOptions
      }),
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
    bintrayPackageLabels := Seq("scala", "aerospike"),
    crossScalaVersions := Versions.supportedScalaVersions,
    unmanagedSourceDirectories in Compile += {
      val sourceDir = (sourceDirectory in Compile).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => sourceDir / "scala-2.13-"
        case _                       => sourceDir / "scala-2.13+"
      }
    }
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(aero)
  .settings(
    version           := "0.1.0",
    scalaVersion      := Versions.scala212,
    scalacOptions     := Settings.scalacOptions,
    organization      := "org.aero",
    bintrayRepository := "aero",
    libraryDependencies ++= Settings.dependencies ++ Settings.extraDependencies
  )
