package com.innoq.framework.impl

import java.io.File

import com.innoq.framework.{InfrastructureHandler, TestLogger, TestingBase}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.sys.process.{ProcessLogger, _}

class DockerInfrastructureHandler(testingBase: TestingBase) extends InfrastructureHandler with BashDockerHandler {

  import testingBase.config

  val debug = config.getBoolean("docker.debug")
  val dockerExecutable = config.getString("docker.executable")
  val dockerConfigPath = config.getString("docker.configPath")

  val setDockerEnv = config.getBoolean("docker.setEnv")
  val dockerProjectName = config.getString("docker.projectName")
  val dockerHost = config.getString("docker.host")
  val dockerForceNoTls = config.getBoolean("docker.forceNoTls")
  val dockerTlsVerify = config.getString("docker.tlsVerify")
  val dockerCertPath = config.getString("docker.certPath")

  val networkName = config.getString("docker.networkName")
  val infrastructureContainers = config.getStringList("docker.infrastructureContainers").asScala
  val applicationContainers = config.getStringList("docker.applicationContainers").asScala
  var applicationsStarted = false

  val applicationStartDuration = config.getInt("docker.applicationStartDuration").millis
  val infrastructureStartDuration = config.getInt("docker.infrastructureStartDuration").millis
  val shutdownDuration = config.getInt("docker.shutdownDuration").millis

  override def initInfrastructure() = {
    printVersion()
    createNetwork(networkName)
  }

  override def createAndStartInfrastructure() = dockerComposeCommand(Some("Starting infrastructure"), "up -d", infrastructureContainers)

  override def waitForInfrastructure() = {
    info(s"Waiting ${infrastructureStartDuration} for infrastructure to be started")
    Thread.sleep(infrastructureStartDuration.toMillis)
  }


  override def createApplications() = dockerComposeCommand(Some("Creating application"), "create", applicationContainers)

  override def startApplications(startDuration: Duration): Unit = {
    if (!applicationsStarted) {
      dockerComposeCommand(Some("Starting application"), "start", applicationContainers)
      applicationsStarted = true
      if (startDuration > 0.millis) {
        info(s"Waiting ${startDuration} for applications to be started")
        Thread.sleep(startDuration.toMillis)
      }
    } else {
      info(s"Not starting applications since already started")
    }
  }

  override def stopApplications() = {
    val title = if (applicationsStarted) Some("Stopping application") else None
    dockerComposeCommand(title, "stop", applicationContainers)
    applicationsStarted = false
  }

  override def stopAndRemoveAll(shutdownDelay: Duration = 0.millis) = {
    dockerComposeCommand(Some("Stopping and removing all"), "down -v", Seq())
    if (shutdownDelay > 0.millis) {
      info(s"Waiting ${shutdownDelay} for all to be stopped")
      Thread.sleep(shutdownDelay.toMillis)
    }
    applicationsStarted = false
  }
}

trait BashDockerHandler extends TestLogger {
  private val ignoreLogger = ProcessLogger(line => (), line => ())
  private val standardOutLogger = ProcessLogger(line => info(line), line => info(line))
  val debug: Boolean

  private def consoleLogger = if (debug) standardOutLogger else ignoreLogger

  val dockerExecutable: String
  val dockerConfigPath: String
  val dockerProjectName: String
  val setDockerEnv: Boolean
  val dockerHost: String
  val dockerForceNoTls: Boolean
  val dockerTlsVerify: String
  val dockerCertPath: String

  def printVersion() = {
    dockerCommand(s"$dockerExecutable -v")
  }

  def createNetwork(networkName: String) = {
    dockerCommand(s"$dockerExecutable network create ${networkName}")
  }

  def dockerComposeCommand(title: Option[String], composeCommand: String, containers: Seq[String]) = {
    val containersString = containers.mkString(" ")
    title.map { t => info(s"${t} containers ${containersString}") }
    val command = s"docker-compose -p ${dockerProjectName} ${composeCommand} ${containersString}"
    dockerCommand(command)
  }

  private def dockerCommand(command: String) = {
    val tlsConfig = dockerForceNoTls match {
      case false => s"""DOCKER_TLS_VERIFY=${dockerTlsVerify} DOCKER_CERT_PATH=${dockerCertPath} """
      case true => ""
    }
    val cmd = setDockerEnv match {
      case true => s"""DOCKER_HOST="${dockerHost}" DOCKER_MACHINE_NAME="projects" ${tlsConfig}${command}"""
      case false => command
    }
    val directory = new File(dockerConfigPath)
    val r = Process(Seq("bash", "-c", cmd), directory).run(consoleLogger)
    val exitCode = r.exitValue()
    if (exitCode != 0) {
      info(s"Failed to execute ${command}: exit code=${exitCode}, directory=${directory.getAbsolutePath}, full command=${cmd}")
      false
    } else {
      true
    }
  }
}

