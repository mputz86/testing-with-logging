package com.innoq.integration

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats, JValue}
import org.scalatra._
import org.scalatra.json._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

class MockController(actorSystem: ActorSystem, interceptionHandler: ActorRef) extends ScalatraServlet with JacksonJsonSupport with FutureSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  protected override def transformRequestBody(body: JValue): JValue = body.camelizeKeys

  protected override def transformResponseBody(body: JValue): JValue = body.underscoreKeys

  protected implicit def executor = scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(5.seconds)

  protected val log = LoggerFactory.getLogger(classOf[MockController])

  log.info("Starting mock controller")

  get("*") {
    interceptRequest("get")
  }

  post("*") {
    interceptRequest("post")
  }

  put("*") {
    interceptRequest("put")
  }

  delete("*") {
    interceptRequest("delete")
  }

  private def interceptRequest(method: String): Future[ActionResult] = {
    val path = multiParams("splat").mkString
    val requestKey = s"${method.toUpperCase} $path"
    log.debug(s"Processing request: ${requestKey}")

    val endpoint = Endpoint(path, method.toUpperCase, List())
    val f = interceptionHandler ? GetInterception(endpoint)
    f.map[ActionResult] {
      case r: Response =>
        log.info(s"Successful request ${requestKey}: $r")
        ActionResult(ResponseStatus(r.statusCode), r.content.getOrElse(""), r.headers.toMap)
      case f =>
        log.info(s"Failed request ${requestKey}: $f")
        InternalServerError(f)
    }
  }
}
