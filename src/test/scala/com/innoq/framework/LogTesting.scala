package com.innoq.framework

import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.Suite

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}

case class WaitFor(level: String, appName: Pattern, message: Pattern, timeout: Duration)

case class Clear()

case class LogMessage(level: String,
                      appName: Option[String],
                      message: String,
                      loggerName: String,
                      threadName: String,
                      levelValue: Int,
                      sourceThread: Option[String],
                      host: String,
                      port: Int,
                      timestamp: String,
                      version: Int)

object LogMessage extends TestLogger {
  def printLogMessage(logMessage: LogMessage) = info(toString(logMessage.level, logMessage.appName.getOrElse("None"), logMessage.message))

  def printLogMessage(level: String, appName: String, message: String) = info(toString(level, appName, message))

  def toString(level: String, appName: String, message: String): String = s"${level}/${appName}: ${message}"
}

object LogTesting {

  val DEBUG = "DEBUG"
  val INFO = "INFO"
  val ERROR = "ERROR"

}

trait LogTesting {
  this: Suite with TestingBase =>

  import scala.concurrent.ExecutionContext.Implicits.global

  private val startUpDelay = config.getInt("integration.applicationStartDuration").millis
  private val waitForFactor = config.getDouble("integration.waitForFactor")

  var testActor: ActorRef
  private var firstWaitFor = true

  def reset(): Unit = {
    firstWaitFor = true
  }

  private def getTimeout(waitForTimeoutBase: FiniteDuration) = {
    val waitForTimeout = waitForTimeoutBase * waitForFactor
    if (firstWaitFor) {
      info(s"Adding startup delay of ${startUpDelay.toMillis}ms to first wait for statement (timeout factor=${waitForFactor})")
      firstWaitFor = false
      startUpDelay + waitForTimeout
    } else {
      waitForTimeout
    }
  }

  private def timed[A](f: => A)(t: (Long, A) => Unit): A = {
    val startTimestamp = new Date().getTime
    val a = f
    val endTimestamp = new Date().getTime
    val diffTimestamp = endTimestamp - startTimestamp
    t(diffTimestamp, a)
    a
  }

  def waitFor(level: String, appName: Pattern, message: Pattern, waitForTimeoutBase: FiniteDuration) = {
    val timeout = getTimeout(waitForTimeoutBase)

    implicit val safetyTimeout = Timeout(2 * timeout.toMillis, TimeUnit.MILLISECONDS)
    val waitFor = WaitFor(level, appName, message, timeout)
    val waitForFuture = (testActor ? waitFor) map {
      case logMessage: LogMessage => LogMessage.toString(logMessage.level, logMessage.appName.getOrElse(""), logMessage.message)
      case r => r
    }

    val logMessagePattern = LogMessage.toString(level, appName.toString, message.toString)

    try {
      timed {
        info(s">> Waiting ${timeout.toMillis}ms for: ${logMessagePattern}")
        Await.result(waitForFuture, timeout)
      } { (t, r) =>
        info(s"<< Continuing test after: time=${t}ms, message=${r}")
      }
    } catch {
      case e: TimeoutException =>
        throw new RuntimeException(s"Message not received after ${timeout.toMillis}ms: ${logMessagePattern}")
      case e: Throwable => throw e
    }
  }

}
