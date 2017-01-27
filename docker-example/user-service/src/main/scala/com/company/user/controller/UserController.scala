package com.company.controller

import com.company.model.CreateUser
import com.company.service.UserService
import com.company.user.service.AccountService
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.slf4j.LoggerFactory

import scala.language.postfixOps

class UserController(userService: UserService, accountService: AccountService) extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  val log = LoggerFactory.getLogger(classOf[UserController])

  log.info("Starting user controller")

  before() {
    contentType = formats("json")
  }

  post("/") {
    val createUserOpt = parsedBody.extractOpt[CreateUser]

    log.debug("Processing create user request")
    createUserOpt.map { createUser =>
      userService.createUser(createUser) match {
        case Right(user) =>
          Ok(user)
        case Left(e) =>
          log.debug(e.getMessage)
          InternalServerError(e)
      }
    }.getOrElse {
      log.debug(s"Failed to parse create user request: body=$parsedBody")
      NotFound("CreateUserParseException")
    }
  }

  get("/:userId") {
    val id = params("userId")

    log.debug(s"Processing get user request: id=$id")
    userService.getUser(id) match {
      case Some(user) =>
        Ok(user)
      case None =>
        log.debug(s"Failed to find user: id=$id")
        NotFound("UserNotFound")
    }
  }

  post("/:userId/credit/:amount") {
    val id = params("userId")
    val amount = params("amount").toInt

    log.debug(s"Processing credit request: user id=$id, amount=$amount")
    accountService.creditUser(id, amount) match {
      case Right(user) =>
        Ok(user)
      case Left(e) =>
        log.debug(e.getMessage)
        NotFound("CreditingFailed")
    }
  }

  post("/:userId/debit/:amount") {
    val id = params("userId")
    val amount = params("amount").toInt

    log.debug(s"Processing debit request: user id=$id, amount=$amount")
    accountService.debitUser(id, amount) match {
      case Right(user) =>
        Ok(user)
      case Left(e) =>
        log.debug(e.getMessage)
        NotFound("DebitingFailed")
    }
  }

}

