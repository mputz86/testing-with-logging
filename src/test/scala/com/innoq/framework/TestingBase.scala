package com.innoq.framework

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

trait TestLogger {
  private val log = LoggerFactory.getLogger("Test")

  def debug(message: Any) = log.debug(message.toString)

  def info(message: Any) = log.info(message.toString)

  def warn(message: Any) = log.info(message.toString)

  def error(message: Any) = log.error(message.toString)

  def error(message: Any, t: Throwable) = log.error(message.toString, t)
}

trait TestingBase extends TestLogger {
  val config = ConfigFactory.load
  val infrastructureConfig = config.getConfig(config.getString("integration.infrastructure"))
}
