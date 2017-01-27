package com.innoq.tests

import com.innoq.framework._
import com.innoq.framework.impl.RedisLogTesting
import com.innoq.framework.utils.HttpInterceptionTesting
import com.innoq.framework.utils.HttpInterceptionTesting.{Endpoint, Interception, Response}
import com.innoq.tests.utils.ShopApiTesting.Rating
import com.innoq.tests.utils._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._


class RatingsTest extends FlatSpec with Matchers with TestingBase with CommonTests with HttpInterceptionTesting with RedisLogTesting with InfrastructureTesting with Users with Shops {

  import LogTesting._

  private def waitForControllers() = {
    waitFor(INFO, Shop, "Starting shop controller".r, 30.seconds)
  }

  "Getting an item" should "return empty ratings list if server is not reachable" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)

    startApplications()

    waitForControllers()

    val coolItem = "CoolItem"

    val getResponse = shopApi.get(coolItem)
    waitFor(DEBUG, Shop, "Ignoring failed external ratings request: .*".r, 10.seconds)

    getResponse.status shouldBe 200
    getResponse.parsedBody.isDefined shouldBe true
    val item = getResponse.parsedBody.get
    item.ratings.isEmpty shouldBe true
  }

  "Getting an item" should "request the external ratings server and include the ratings from the server" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)

    val coolItem = "CoolItem"
    shopApi.fakeRating("CoolItem", Seq(
      Rating(coolItem, "CustomerCareAgency", 4),
      Rating(coolItem, "Coolness.org", 10)
    ))

    startApplications()

    waitForControllers()

    val getResponse = shopApi.get(coolItem)
    getResponse.status shouldBe 200
    getResponse.parsedBody.isDefined shouldBe true
    val item = getResponse.parsedBody.get
    item.ratings.size shouldBe 2
  }

  "Getting an item" should "ignore all status codes different from 200 from backend" in withApplicationStop {
    implicit val shopApi = new ShopApiTesting(this)

    val coolItem = "CoolItem"
    setInterception(Interception(
      Endpoint(s"ratings/$coolItem", "GET", List.empty),
      Response(403, None, List.empty)
    ))

    startApplications()

    waitForControllers()

    val getResponse = shopApi.get(coolItem)
    getResponse.status shouldBe 200
    getResponse.parsedBody.isDefined shouldBe true
    val item = getResponse.parsedBody.get
    item.ratings.size shouldBe 0
  }
}
