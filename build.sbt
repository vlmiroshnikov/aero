name := "aero"

version := "0.1"
scalaVersion := "2.12.3"


libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.aerospike" % "aerospike-client" % "4.0.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.6"
)