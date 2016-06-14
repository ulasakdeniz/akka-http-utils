val projectName = "hakker"

val akkaVersion = "2.4.5"
val scalaTestVersion = "2.2.6"

val commonSettings = Seq(
  organization := s"com.ulasakdeniz.$projectName",
  name := projectName,
  version := "1.0",
  scalaVersion := "2.11.8",
  publishMavenStyle := true,
  parallelExecution in Test := false,
  publishArtifact in Test := false,
  scalacOptions := Seq("-unchecked", "-encoding", "utf8"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.mockito" % "mockito-core" % "1.10.19"
  )
)

lazy val root = Project(projectName, file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false)
  .aggregate(core, sample)

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(name := s"$projectName-core")

lazy val sample = (project in file("sample"))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false)
  .settings(libraryDependencies ++= Seq(
    "net.debasishg" %% "redisclient" % "3.0"
  ))
  .dependsOn(core)
