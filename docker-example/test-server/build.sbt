lazy val scalaTestVersion = "2.2.4"
lazy val scalatraVersion = "2.4.0"

lazy val root = (project in file(".")).enablePlugins(DockerPlugin).settings(
  organization := "com.innoq",
  name := "Test Server",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
    "com.typesafe" % "config" % "1.3.0",
    "com.typesafe.akka" %% "akka-actor" % "2.4.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.5" % "runtime",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.6",
    "org.codehaus.janino" % "janino" % "2.7.8",
    "org.eclipse.jetty" % "jetty-webapp" % "9.2.10.v20150310" % "compile",
    "org.json4s" %% "json4s-jackson" % "3.3.0",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.scalatra" %% "scalatra" % scalatraVersion,
    "org.scalatra" %% "scalatra-json" % scalatraVersion
  ),
  docker <<= docker.dependsOn(prepareWebapp, test in Test),
  dockerfile in docker := {
    val jarFile: File = sbt.Keys.`packageBin`.in(Compile, packageBin).value
    val classpath = (managedClasspath in Compile).value ++ (managedClasspath in Runtime).value
    val appConf = (resources in Compile).value
    val conf = appConf.find(_.name == "application.conf")
    val selectedMainClass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
    val jarTarget = s"/app/${jarFile.getName}"
    val webappDir = (webappDest in webapp).value

    val classpathString = classpath.files.map("/app/lib/" + _.getName).mkString(":") + ":" + jarTarget

    new Dockerfile {
      from("java")
      add(classpath.files, "/app/lib/")
      add(webappDir, "/app/webapp")
      runRaw("rm -rf /app/webapp/WEB-INF/lib")
      runRaw("rm -rf /app/webapp/WEB-INF/classes")
      conf.foreach(add(_, "/app/config/application.conf"))
      add(jarFile, jarTarget)
      workDir("/app")
      expose(8080)
      cmdRaw("java -cp " + classpathString + "  " + selectedMainClass + " -Dconfig.file=/app/config/application.conf")
    }
  },
  imageNames in docker := Seq(
    ImageName(s"testing-with-logging/${name.value.replace(" ", "-").toLowerCase}:latest")
  )
).settings(jetty(): _*)
