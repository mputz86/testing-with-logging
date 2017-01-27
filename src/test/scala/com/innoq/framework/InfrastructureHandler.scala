package com.innoq.framework

import scala.concurrent.duration.{Duration, FiniteDuration, _}

case class InfrastructureException(msg: String) extends Exception(msg)

trait InfrastructureHandler {
  val applicationStartDuration: FiniteDuration
  val infrastructureStartDuration: FiniteDuration
  val shutdownDuration: FiniteDuration

  def initInfrastructure(): Boolean

  def createAndStartInfrastructure(): Boolean

  def waitForInfrastructure()

  def createApplications(): Boolean

  def startApplications(startDuration: Duration): Unit

  def stopApplications(): Unit

  def stopAndRemoveAll(shutdownDelay: Duration = 0.millis): Unit
}

