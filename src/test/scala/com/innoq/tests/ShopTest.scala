package com.innoq.tests

import com.innoq.framework._
import com.innoq.framework.impl.RedisLogTesting
import com.innoq.framework.utils.HttpInterceptionTesting
import com.innoq.tests.utils._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._


class ShopTest extends FlatSpec with Matchers with TestingBase with CommonTests with HttpInterceptionTesting with RedisLogTesting with InfrastructureTesting with Users with Shops {

  import LogTesting._

  private def waitForControllers() = {
    waitFor(INFO, Shop, "Starting shop controller".r, 30.seconds)
    waitFor(INFO, Users, "Starting user controller".r, 30.seconds)
  }

  "Buying an item" should "fail if user has not credited his account yet" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()

    val user = createUser()
    val userId = user.id

    val coolItem = "CoolItem"
    val coolItemPrice = 523

    shopApi.buy(userId, coolItem, coolItemPrice)
    waitFor(DEBUG, Shop, "Processing buy request.*".r, 10.seconds)
    waitFor(DEBUG, Users, s"Processing debit request: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)
    waitFor(DEBUG, Users, s"Debiting user: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)
    waitFor(DEBUG, Users, s"Not enough funds: balance=0, amount=.*, user id=${user.id}".r, 10.seconds)
    waitFor(DEBUG, Shop, "Failed to debit user: .*".r, 10.seconds)
  }

  "Buying an item" should "succeed if user has enough balance" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()
    val user = createUser()
    creditUser(user.id, 100)

    val coolItem = "CoolItem"
    val coolItemPrice = 23

    val buyResponse = shopApi.buy(user.id, coolItem, coolItemPrice)
    waitFor(DEBUG, Shop, "Processing buy request.*".r, 10.seconds)
    waitFor(DEBUG, Users, s"Processing debit request: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)
    waitFor(DEBUG, Users, s"Debiting user: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)

    buyResponse.status shouldBe 200
  }

  "Buying an item" should "fail if user has not enough balance" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)
    implicit val userApi = new UserApiTesting(this)

    startApplications()
    waitForControllers()

    val user = createUser()
    creditUser(user.id, 100)

    val coolItem = "CoolThing"
    val coolItemPrice = 523

    val buyResponse = shopApi.buy(user.id, coolItem, coolItemPrice)
    waitFor(DEBUG, Shop, "Processing buy request.*".r, 10.seconds)
    waitFor(DEBUG, Users, s"Processing debit request: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)
    waitFor(DEBUG, Users, s"Debiting user: user id=${user.id}, amount=$coolItemPrice".r, 10.seconds)
    waitFor(DEBUG, Users, s"Not enough funds: balance=.*, amount.*, user id=${user.id}".r, 10.seconds)
    waitFor(DEBUG, Shop, s"Failed to debit user: user id=${user.id}, .*".r, 10.seconds)
    buyResponse.status shouldBe 500
  }
}
