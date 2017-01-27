package com.company.user.api

import com.company.model.User
import org.json4s.jackson.Serialization.{read, write}
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class Amount(amount: Int)

class UserApi(client: WSClient, url: String) {

  import scala.concurrent.ExecutionContext.Implicits.global

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  def buy(userId: String, amount: Int): Future[Either[Throwable, User]] =
    client
      .url(s"$url/users/$userId/debit/$amount")
      .post(write(Amount(amount)))
      .map { response =>
        response.status match {
          case 200 =>
            Right(read[User](response.body))
          case status =>
            Left(new Exception(s"Failed to debit user: user id=$userId, amount=$amount, status code=$status, response=${response.body}"))
        }
      }.recover {
        case e: Throwable =>
          Left(new Exception(s"Failed to connect to user service: user id=$userId, exception=${e.getMessage}"))
      }
}
