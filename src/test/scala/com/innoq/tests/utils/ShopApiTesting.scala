package com.innoq.tests.utils

import com.innoq.framework._
import com.innoq.framework.utils.HttpInterceptionTesting.{Endpoint, Interception, Response}
import com.innoq.framework.utils.{HttpInterceptionTesting, Methods}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

object ShopApiTesting {

  case class UserBuyRequest(userId: String)

  case class Item(name: String, ratings: Seq[Rating])

  case class Rating(name: String, provider: String, rating: Int)

}

class ShopApiTesting(test: TestingBase with HttpInterceptionTesting with LogTesting) {

  import ShopApiTesting._
  import UserApiTesting.User

  implicit private val formats = DefaultFormats

  private val shopApiUrl = test.infrastructureConfig.getString("services.shop.url")

  def get(name: String) = {
    val r = test.requestAndWait(s"$shopApiUrl/shop/items/$name", Methods.GET)
    test.parseResponse[Item](r)
  }

  def fakeRating(name: String, ratings: Seq[Rating] = Seq.empty): Unit = {
    test.setInterception(Interception(
      Endpoint(s"/ratings/$name", "GET", List.empty),
      Response(200, Some(write(ratings)), List.empty)
    ))
  }

  def buy(userId: String, name: String, value: Int) = {
    val userBuyRequest = UserBuyRequest(userId)
    val json = write(userBuyRequest)
    val r = test.requestAndWait(s"$shopApiUrl/shop/items/$name/$value", Methods.POST, json)
    test.parseResponse[User](r)
  }

}
