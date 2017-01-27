package com.innoq.tests.utils

import com.innoq.framework.TestingBase
import com.innoq.framework.impl.RedisLogTesting
import com.innoq.framework.utils.HttpInterceptionTesting
import org.scalatest.Matchers

trait CommonTests extends Matchers {
  this: TestingBase with HttpInterceptionTesting with RedisLogTesting =>
}
