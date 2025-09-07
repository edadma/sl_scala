ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.2"

lazy val root = (project in file("."))
  .settings(
    name := "sl_scala",
    idePackagePrefix := Some("io.github.edadma.sl_scala")
  )
