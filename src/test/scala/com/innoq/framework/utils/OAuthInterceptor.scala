package com.innoq.framework.utils

import com.innoq.framework.impl.RedisLogTesting
import com.innoq.framework.utils.HttpInterceptionTesting.{Endpoint, Interception, Response}
import org.scalatest.{Suite, SuiteMixin}

trait OAuthInterceptor extends SuiteMixin {
  this: Suite with RedisLogTesting with HttpInterceptionTesting =>

  val oAuthTokenInterception = Interception(Endpoint("/o/token/", "POST", List()),
    Response(200, Some("""{ "access_token": "abc", "refresh_token": "abc" }"""), List("Content-Type" -> "application/json")))

  abstract override def withFixture(test: NoArgTest) = {
    setInterception(oAuthTokenInterception)
    super.withFixture(test)
  }
}

