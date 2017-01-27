package com.innoq.framework

import com.innoq.framework.impl.DockerInfrastructureHandler
import org.scalatest.{Exceptional, Suite, SuiteMixin}

import scala.concurrent.duration._

trait InfrastructureTesting extends SuiteMixin {
  this: Suite with TestingBase =>

  val infrastructureEnvironment = config.getString("integration.infrastructure")

  implicit lazy val infrastructureHandler: InfrastructureHandler = infrastructureEnvironment match {
    case "docker" =>
      info(s"Using docker infrastructure")
      new DockerInfrastructureHandler(this)
    case i =>
      info(s"Using docker infrastructure since ${i} unknown")
      new DockerInfrastructureHandler(this)
  }

  abstract override def withFixture(test: NoArgTest) = {
    infrastructureHandler.stopAndRemoveAll(infrastructureHandler.shutdownDuration)

    if (infrastructureHandler.initInfrastructure() && infrastructureHandler.createAndStartInfrastructure() && infrastructureHandler.createApplications()) {
      if (infrastructureHandler.infrastructureStartDuration > 0.millis) {
        infrastructureHandler.waitForInfrastructure()
      }
      try {
        super.withFixture(test)
      } finally {
        infrastructureHandler.stopAndRemoveAll()
      }
    } else {
      try {
        infrastructureHandler.stopAndRemoveAll()
      } catch {
        case t: Throwable => ;
      }
      Exceptional(InfrastructureException("Failed to execute infrastructure command"))
    }
  }

  def startApplications(): Unit = infrastructureHandler.startApplications(infrastructureHandler.applicationStartDuration)

  def withApplicationStop(test: => Any) = {
    try {
      test
    } finally {
      infrastructureHandler.stopApplications()
    }
  }

  def withApplicationStartStop(test: => Any) = {
    startApplications()
    try {
      test
    } finally {
      infrastructureHandler.stopApplications()
    }
  }
}
