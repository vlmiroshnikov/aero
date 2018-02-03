import sbt.Keys._

lazy val `aero` = project
  .in(file("."))
  .settings(
    version := "0.1.1",
    scalaVersion := "2.12.4",
    scalacOptions := Settings.scalacOptions,
    libraryDependencies ++= Settings.dependencies,
    organization := "org.aero",
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
