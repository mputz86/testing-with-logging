package com.company.repository

import com.company.model.{CreateUser, User}

object UserRepository {

  def userExistsException(name: String) = new Exception(s"User exists already: name=$name")

  def userNotFoundException(id: String) = new Exception(s"User not found: id=$id")

  def notEnoughBalanceException(id: String, amount: Int, balance: Int) = new Exception(s"Not enough funds: balance=$balance, amount=$amount, user id=$id")

}

class UserRepository {

  import UserRepository._

  var idCounter = 1
  var users = Map.empty[String, User]

  private def newId() = {
    val id = idCounter.toString
    idCounter += 1
    id
  }

  private def existsUser(name: String) =
    users.exists { case (_, user) => user.name == name }

  private def addUser(createUser: CreateUser): User = {
    val id = newId()
    val newUser = User.create(id, createUser.name, createUser.password)
    users = users + (id -> newUser)
    newUser
  }

  private def updateUserBalance(id: String, amount: Int): Either[Throwable, User] = {
    val user = users.get(id)
    user match {
      case Some(u) if u.balance + amount >= 0 =>
        val newUser = u.copy(balance = u.balance + amount)
        users = users + (id -> newUser)
        Right(newUser)
      case Some(u) =>
        Left(notEnoughBalanceException(id, amount, u.balance))
      case None =>
        Left(userNotFoundException(id))
    }
  }

  def createUser(createUser: CreateUser): Either[Throwable, User] = {
    if (!existsUser(createUser.name)) {
      Right(addUser(createUser))
    } else {
      Left(userExistsException(createUser.name))
    }
  }

  def getUser(userId: String): Option[User] = users.get(userId)

  def creditUser(userId: String, amount: Int): Either[Throwable, User] =
    updateUserBalance(userId, amount)

  def debitUser(userId: String, amount: Int): Either[Throwable, User] =
    updateUserBalance(userId, -amount)
}
