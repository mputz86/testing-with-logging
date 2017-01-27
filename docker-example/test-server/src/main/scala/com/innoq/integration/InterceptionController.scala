package com.innoq.integration

import akka.actor.{ActorRef, ActorSystem}
import org.json4s.{DefaultFormats, Formats, JValue}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{FutureSupport, Ok, ScalatraServlet}
import org.slf4j.LoggerFactory

class InterceptionController(actorSystem: ActorSystem, interceptionHandler: ActorRef) extends ScalatraServlet with JacksonJsonSupport with FutureSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  protected override def transformRequestBody(body: JValue): JValue = body.camelizeKeys

  protected override def transformResponseBody(body: JValue): JValue = body.underscoreKeys

  protected implicit def executor = scala.concurrent.ExecutionContext.Implicits.global

  protected val log = LoggerFactory.getLogger(classOf[InterceptionController])

  log.info("Starting interception controller")

  before() {
    contentType = formats("json")
  }

  get("/*") {
    Ok
  }

  put("/") {
    val interception = parsedBody.extract[Interception]
    log.info(s"Setting interception: $interception")
    interceptionHandler ! SetInterception(interception)
    Ok
  }

  post("/") {
    val interception = parsedBody.extract[Interception]
    log.info(s"Adding interception: $interception")
    interceptionHandler ! AddInterception(interception)
    Ok
  }

  delete("/*") {
    log.info(s"Deleting interceptions")
    interceptionHandler ! DeleteInterceptions()
    Ok
  }
}
