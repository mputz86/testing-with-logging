package com.innoq.tests

import com.innoq.framework.utils.HttpInterceptionTesting
import com.innoq.framework.{LogTesting, TestingBase}
import org.scalatest.{FlatSpec, Matchers}

trait Shops extends FlatSpec with Matchers with TestingBase with HttpInterceptionTesting {
  this: LogTesting =>

  val Shop = "shop".r

}
