package com.innoq.framework.impl

import java.net.InetSocketAddress
import java.util.regex.Pattern

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.innoq.framework._
import org.json4s._
import org.json4s.native.JsonMethods.parse
import org.scalatest._
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{Message, PMessage}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.language.implicitConversions
import scala.util.matching.Regex

case class RedisLogMessage(
                            level: String,
                            appName: Option[String],
                            message: String,
                            loggerName: String,
                            threadName: String,
                            levelValue: Int,
                            hOSTNAME: String,
                            sourceThread: Option[String],
                            akkaTimestamp: Option[String],
                            akkaSource: Option[String],
                            sourceActorSystem: Option[String],
                            host: String,
                            port: Int,
                            `@timestamp`: String,
                            `@version`: Int,
                            `type`: String)

object LogMessageProcessingActor {
  val channels = Seq("logs")
  val patterns = Seq()
}

class RedisLogMessageSubscriber(testActor: ActorRef, host: String, port: Int, connectedPromise: Option[Promise[Boolean]]) extends RedisSubscriberActor(new InetSocketAddress(host, port),
  LogMessageProcessingActor.channels, LogMessageProcessingActor.patterns, None, (connected: Boolean) => connectedPromise.map(_ trySuccess connected)) with TestLogger {

  override val address: InetSocketAddress = new InetSocketAddress(host, port)
  implicit val formats = DefaultFormats

  private def toLogMessage(redisLogMessage: RedisLogMessage) =
    LogMessage(
      redisLogMessage.level,
      redisLogMessage.appName,
      redisLogMessage.message,
      redisLogMessage.loggerName,
      redisLogMessage.threadName,
      redisLogMessage.levelValue,
      redisLogMessage.sourceThread,
      redisLogMessage.host,
      redisLogMessage.port,
      redisLogMessage.`@timestamp`,
      redisLogMessage.`@version`
    )

  def onMessage(message: Message) = {
    try {
      val json = parse(message.data.utf8String)
      val redisLogMessage = json.camelizeKeys.extract[RedisLogMessage]
      testActor ! toLogMessage(redisLogMessage)
    } catch {
      case e: Throwable => info(s"Failed to parse log message: exception=${e.getMessage}, message=${message}")
    }
  }

  def onPMessage(pmessage: PMessage) = {
    info(pmessage)
  }
}

trait RedisLogTesting extends SuiteMixin with LogTesting {
  this: Suite with TestingBase =>

  private val redisHost = infrastructureConfig.getString("redis.host")
  private val redisPort = infrastructureConfig.getInt("redis.port")
  private val verboseLogging = config.getBoolean("integration.verboseLogging")

  var testActor: ActorRef = _

  implicit def regexToPattern(regex: Regex): Pattern = regex.pattern

  abstract override def withFixture(test: NoArgTest) = {
    implicit val actorSystem = ActorSystem()
    reset()

    testActor = actorSystem.actorOf(Props(classOf[LogMessageTestingActor], verboseLogging))

    info(s"Starting redis log message subscriber on ${redisHost}:${redisPort}")
    val connectedPromise = Promise[Boolean]()
    actorSystem.actorOf(Props(classOf[RedisLogMessageSubscriber], testActor, redisHost, redisPort, Some(connectedPromise)))

    try {
      debug(s"Waiting for redis connection")
      Await.result(connectedPromise.future, Timeout(20.seconds).duration) match {
        case true => super.withFixture(test)
        case false =>
          info("Failed to connect to redis")
          throw (new RuntimeException())
      }
    } finally {
      actorSystem.terminate()
    }
  }

}

