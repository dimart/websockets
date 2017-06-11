
val orgSettings = Seq(
  organization := "org.websockets",
  version := "0.1"
)

lazy val baseSettings = Seq(
  scalaVersion := "2.12.2"
) ++ orgSettings

lazy val server = project
  .settings(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.5"
    )
  )

lazy val websockets = project
  .in(file("."))
  .aggregate(
    server
  )
  .settings(orgSettings)
