package com.company.model

case class User(id: String, name: String, password: String, balance: Int, createdAt: Long)

object User {
  def create(id: String, name: String, password: String) =
    new User(id, name, password, 0, System.currentTimeMillis())
}
