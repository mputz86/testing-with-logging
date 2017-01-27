
lazy val scalaTestVersion = "2.2.4"

lazy val root = (project in file(".")).enablePlugins(DockerPlugin).settings(
  organization := "com.innoq",
  name := "Test Framework",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"),
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
    "com.github.etaty" %% "rediscala" % "1.6.0",
    "com.typesafe" % "config" % "1.3.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.4" % "runtime",
    "com.typesafe.akka" %% "akka-actor" % "2.4.5",
    "com.typesafe.play" %% "play-ws" % "2.5.2",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
    "org.eclipse.jetty" % "jetty-webapp" % "9.2.10.v20150310" % "compile",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.json4s" %% "json4s-native" % "3.3.0",
    "org.json4s" %% "json4s-jackson" % "3.3.0"
  )
).settings(jetty(): _*)

parallelExecution in Test := false
