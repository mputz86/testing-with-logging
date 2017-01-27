package com.innoq.tests

import com.innoq.framework.TestingBase
import com.innoq.framework.impl.RedisLogTesting
import com.innoq.tests.utils.UserApiTesting
import com.innoq.tests.utils.UserApiTesting.User
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

trait Users extends FlatSpec with Matchers with TestingBase with RedisLogTesting {

  import com.innoq.framework.LogTesting._

  val Users = "user".r

  def createUser(userName: String = "Hans", password: String = "1234")(implicit userApi: UserApiTesting): User = {
    val createUserResponse = userApi.createUser(userName, password)
    createUserResponse.status shouldBe 200

    waitFor(DEBUG, Users, "Processing create user request".r, 10.seconds)
    waitFor(DEBUG, Users, s"Creating user: name=$userName".r, 10.seconds)

    createUserResponse.parsedBody.isDefined shouldBe true
    val user = createUserResponse.parsedBody.get
    user.name shouldBe userName
    user
  }

  def getUser(userId: String)(implicit userApi: UserApiTesting): User = {
    val returnedUser = userApi.getUser(userId)
    returnedUser.status shouldBe 200
    waitFor(DEBUG, Users, "Processing get user request: .*".r, 10.seconds)
    returnedUser.parsedBody.isDefined shouldBe true
    returnedUser.parsedBody.get
  }

  def creditUser(userId: String, amount: Int)(implicit userApi: UserApiTesting): User = {
    userApi.credit(userId, amount)
    waitFor(DEBUG, Users, s"Processing credit request: user id=$userId, amount=$amount".r, 10.seconds)
    waitFor(DEBUG, Users, s"Crediting user: user id=$userId, amount=$amount".r, 10.seconds)
    val userAfterCrediting = getUser(userId)
    userAfterCrediting.balance should be >= amount
    userAfterCrediting
  }

  def debit(userId: String, amount: Int)(implicit userApi: UserApiTesting): User = {
    userApi.debit(userId, amount)
    waitFor(DEBUG, Users, s"Processing debit request: user id=$userId, amount=$amount".r, 10.seconds)
    waitFor(DEBUG, Users, s"Debiting user: user id=$userId, amount=$amount".r, 10.seconds)
    val userAfterDebiting = getUser(userId)
    userAfterDebiting.balance should be >= 0
    userAfterDebiting
  }
}
