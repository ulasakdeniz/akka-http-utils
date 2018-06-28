val projectName = "akka-http-utils"

val akkaVersion      = "2.5.13"
val akkaHttpVersion  = "10.1.3"
val scalaTestVersion = "3.0.0"

val commonSettings = Seq(
  organization := s"com.ulasakdeniz.$projectName",
  name := projectName,
  description := "akka-http utilities",
  licenses := Seq("MIT" -> url("https://github.com/ulasakdeniz/hakker/blob/master/LICENSE")),
  homepage := Some(url("https://github.com/ulasakdeniz/hakker")),
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.12.6",
  publishMavenStyle := true,
  bintrayOrganization in bintray := None,
  parallelExecution in Test := false,
  publishArtifact in Test := false,
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
)

lazy val akkaHttpUtils = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(commonSettings: _*)
  .aggregate(auth)

lazy val auth = (project in file("auth"))
  .settings(commonSettings: _*)
  .settings(publishArtifact := true)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor",
      "com.typesafe.akka" %% "akka-stream",
      "com.typesafe.akka" %% "akka-stream-testkit"
    ).map(_ % akkaVersion) ++Seq(
      "com.typesafe.akka"  %% "akka-http",
      "com.typesafe.akka" %% "akka-http-testkit"
    ).map(_ % akkaHttpVersion) ++ Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.mockito" % "mockito-core" % "1.10.19" % "test"
    )
  )
  .settings(name := "auth")
