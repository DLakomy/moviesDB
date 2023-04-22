val tapirVersion = "1.2.10"

ThisBuild / scalacOptions ++=
  Seq("-Xmax-inlines", "256", "-Xfatal-warnings", "-deprecation")

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "moviesdb",
    version := "0.1.0-SNAPSHOT",
    organization := "moviesdb",
    scalaVersion := "3.2.2",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % "0.23.18",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.6",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "org.scalameta" %% "munit" % "1.0.0-M7" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.8.13" % Test,
      "io.circe" %% "circe-core" % "0.14.5"
    )
  )
)
