import sbt.Keys._

lazy val `aero` = project
  .in(file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.12.4",
    scalacOptions := Settings.scalacOptions,
    libraryDependencies ++= Settings.dependencies
  )
