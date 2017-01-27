package com.company.service

import com.company.model.User
import com.company.user.api.{ExternalRatingsApi, UserApi}
import com.company.user.model.Item
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class ShopService(userApi: UserApi, externalRatingsApi: ExternalRatingsApi) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val log = LoggerFactory.getLogger(classOf[ShopService])

  def buyItem(userId: String, name: String, value: Int): Future[Either[Throwable, User]] = {
    log.debug(s"Debiting user: name=$name, value=$value, user id=$userId")
    userApi.buy(userId, value)
  }

  def getItem(name: String): Future[Either[Throwable, Item]] = {
    log.debug(s"Getting item details: name=$name")
    externalRatingsApi.getRatings(name).map {
      _ match {
        case Right(ratings) =>
          Right(Item(name, ratings))
        case Left(e) =>
          log.debug(s"Ignoring failed external ratings request: name=$name, ignored exception=$e")
          Right(Item(name, Seq.empty))
      }
    }
  }
}
