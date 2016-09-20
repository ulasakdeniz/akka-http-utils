val projectName = "hakker"

val akkaVersion = "2.4.9"
val scalaTestVersion = "2.2.6"
val circeVersion = "0.4.1"

resolvers += Resolver.jcenterRepo

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

val commonSettings = Seq(
  organization := s"com.ulasakdeniz.$projectName",
  name := projectName,
  description := "akka-http web framework",
  licenses := Seq("MIT" -> url("https://github.com/ulasakdeniz/hakker/blob/master/LICENSE")),
  homepage := Some(url("https://github.com/ulasakdeniz/hakker")),
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  publishMavenStyle := true,
  bintrayOrganization in bintray := None,
  parallelExecution in Test := false,
  publishArtifact in Test := false,
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
)

lazy val hakker = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(commonSettings: _*)
  .aggregate(core, ws)

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(publishArtifact := true)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor",
      "com.typesafe.akka" %% "akka-stream",
      "com.typesafe.akka" %% "akka-http-core",
      "com.typesafe.akka" %% "akka-http-experimental",
      "com.typesafe.akka" %% "akka-stream-testkit",
      "com.typesafe.akka" %% "akka-http-testkit"
    ).map(_ % akkaVersion) ++ Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.mockito" % "mockito-core" % "1.10.19",
      "de.heikoseeberger" %% "akka-http-circe" % "1.7.0"
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )
  .settings(name := s"$projectName-core")

lazy val ws = (project in file("ws"))
  .settings(commonSettings: _*)
  .settings(publishArtifact := true)
  .settings(name := s"$projectName-ws")
  .dependsOn(core, core % "test->test")