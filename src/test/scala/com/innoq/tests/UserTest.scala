package com.innoq.tests

import com.innoq.framework._
import com.innoq.framework.impl.RedisLogTesting
import com.innoq.framework.utils.HttpInterceptionTesting
import com.innoq.tests.utils._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._


class UserTest extends FlatSpec with Matchers with TestingBase with CommonTests with HttpInterceptionTesting with RedisLogTesting with InfrastructureTesting with Users {

  import LogTesting._

  private def waitForControllers() = {
    waitFor(INFO, Users, "Starting user controller".r, 30.seconds)
  }

  "User" should "be created if not exists" in withApplicationStop {
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()

    val userName = "Hans"
    val user = createUser(userName)
    user.name shouldBe userName
  }

  "User creation" should "fail if user name is already in repository" in withApplicationStop {
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()

    val userName = "Hans"
    createUser("Hans")

    val secondUserCreationResponse = userApi.createUser(userName, "1234")
    secondUserCreationResponse.status shouldBe 500
    waitFor(DEBUG, Users, "Processing create user request".r, 10.seconds)
    waitFor(DEBUG, Users, s"Creating user: name=$userName".r, 10.seconds)
    waitFor(DEBUG, Users, s"User exists already: name=$userName.*".r, 10.seconds)
  }

  "User" should "have balance 0 after creation" in withApplicationStop {
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()
    val user = createUser()

    val returnedUser = getUser(user.id)
    returnedUser.balance shouldBe 0
  }

  "User balance" should "increase from 0 to 20 and from 20 to 100 if credited with 20 and 80 successively" in withApplicationStop {
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()
    val user = createUser()

    creditUser(user.id, 20)
    creditUser(user.id, 80)
    val userAfterCrediting = getUser(user.id)
    userAfterCrediting.balance shouldBe 100
  }

  "User balance" should "decrease to 80 when debiting 20 after crediting 100" in withApplicationStop {
    implicit val userApi = new UserApiTesting(this)

    startApplications()

    waitForControllers()
    val user = createUser()

    creditUser(user.id, 100)
    debit(user.id, 20)
    val userAfterCrediting80 = getUser(user.id)
    userAfterCrediting80.balance shouldBe 80
  }
}
