package com.innoq.tests.utils

import com.innoq.framework.TestingBase
import com.innoq.framework.utils.{HttpInterceptionTesting, Methods}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._

object UserApiTesting {

  implicit private val formats = Serialization.formats(NoTypeHints)

  case class CreateUser(name: String, password: String)

  case class User(id: String, name: String, password: String, balance: Int)

}

class UserApiTesting(test: TestingBase with HttpInterceptionTesting) {

  import UserApiTesting._

  private val userApiUrl = test.infrastructureConfig.getString("services.user.url")

  def createUser(name: String, password: String) = {
    val createUser = CreateUser(name, password)
    val json = write(createUser)
    val r = test.requestAndWait(s"$userApiUrl/users/", Methods.POST, json)
    test.parseResponse[User](r)
  }

  def getUser(id: String) = {
    val r = test.requestAndWait(s"$userApiUrl/users/$id", Methods.GET)
    test.parseResponse[User](r)
  }

  def credit(userId: String, amount: Int) = {
    val r = test.requestAndWait(s"$userApiUrl/users/$userId/credit/$amount", Methods.POST)
    test.parseResponse[User](r)
  }

  def debit(userId: String, amount: Int) = {
    val r = test.requestAndWait(s"$userApiUrl/users/$userId/debit/$amount", Methods.POST)
    test.parseResponse[User](r)
  }
}
