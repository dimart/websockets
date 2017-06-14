
val orgSettings = Seq(
  organization := "org.websockets",
  version := "0.1"
)

lazy val baseSettings = Seq(
  scalaVersion := "2.12.2"
) ++ orgSettings

lazy val client = project
  .settings(baseSettings)
  .settings(
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.6"
//      "org.ensime" %% "jerky" % "2.0.0-SNAPSHOT"
    )
  )

lazy val server = project
  .settings(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.6"
    )
  )

lazy val websockets = project
  .in(file("."))
  .aggregate(
    client,
    server
  )
  .settings(orgSettings)
