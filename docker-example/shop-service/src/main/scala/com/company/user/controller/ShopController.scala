package com.company.controller

import com.company.model.UserBuyRequest
import com.company.service.ShopService
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ShopController(shopService: ShopService) extends ScalatraServlet with FutureSupport with JacksonJsonSupport {

  override protected implicit def executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  val log = LoggerFactory.getLogger(classOf[ShopController])

  log.info("Starting shop controller")

  before() {
    contentType = formats("json")
  }

  post("/items/:name/:value") {
    val name = params("name")
    val value = params("value").toInt

    log.debug(s"Processing buy request: name=$name, value=$value")
    val buyRequestOpt = parsedBody.extractOpt[UserBuyRequest]

    buyRequestOpt.map { userBuyRequest =>
      shopService.buyItem(userBuyRequest.userId, name, value).map {
        _ match {
          case Right(v) =>
            Ok(v)
          case Left(e) =>
            log.debug(e.getMessage)
            InternalServerError(e)
        }
      }
    }.getOrElse {
      log.debug(s"Failed to parse buy request: body=$parsedBody, name=$name")
      Future.successful(NotFound("FailedToBuyException"))
    }
  }

  get("/items/:name") {
    val name = params("name")

    log.debug(s"Processing get item request: name=$name")
    shopService.getItem(name).map { r =>
      r match {
        case Right(v) =>
          Ok(v)
        case Left(e) =>
          log.debug(e.getMessage)
          InternalServerError(e)
      }
    }
  }
}

