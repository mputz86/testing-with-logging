package com.innoq.integration

import akka.actor.Actor
import org.slf4j.LoggerFactory

import scala.collection.mutable.Map

case class Endpoint(path: String, method: String, headers: List[(String, String)])

case class Response(statusCode: Int, content: Option[String], headers: List[(String, String)])

case class Interception(endpoint: Endpoint, response: Response)

case class AddInterception(interception: Interception)

case class SetInterception(interception: Interception)

case class GetInterception(endpoint: Endpoint)

case class DeleteInterceptions()

case class NoInterceptionFound()

class InterceptionHandler extends Actor {

  protected val log = LoggerFactory.getLogger(classOf[InterceptionHandler])

  val interceptions = Map[String, Seq[Interception]]()
  val errorResponse = Response(500, None, List())

  override def receive = {
    case AddInterception(interception) =>
      val key = endpointKey(interception.endpoint)
      val endpointInterceptions = interceptions.getOrElse(key, Seq[Interception]())
      val newEndpointInterceptions = endpointInterceptions :+ interception
      interceptions += (key -> newEndpointInterceptions)
    case SetInterception(interception) =>
      val key = endpointKey(interception.endpoint)
      interceptions += (key -> Seq(interception))
    case GetInterception(endpoint) =>
      val key = endpointKey(endpoint)
      interceptions.get(key) match {
        case Some(interception :: tail) =>
          // TODO not for now
          // interceptions += (key -> tail)
          sender ! interception.response
        case Some(Seq()) =>
          log.debug(s"Could not find ${key} in ${interceptions.mkString(", ")}")
          interceptions -= key
          sender ! errorResponse
        case None =>
          log.debug(s"Could not find ${key} in ${interceptions.mkString(", ")}")
          sender ! errorResponse
      }
    case DeleteInterceptions() =>
      interceptions.clear()
    case e =>
      log.info(s"Cannot find event: $e")
  }

  private def endpointKey(endpoint: Endpoint) = s"${endpoint.method} ${endpoint.path}"
}
