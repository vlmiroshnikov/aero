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
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    pomExtra :=
      <developers>
        <developer>
          <id>vlmiroshnikov</id>
          <name>Vyacheslav Miroshnikov</name>
        </developer>
      </developers>,
    bintrayPackageLabels := Seq("scala", "aerospike")
  )
