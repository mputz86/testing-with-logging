package com.company.service

import com.company.model.{CreateUser, User}
import com.company.repository.UserRepository
import org.slf4j.LoggerFactory

class UserService(userRepository: UserRepository) {

  val log = LoggerFactory.getLogger(classOf[UserService])

  def createUser(createUser: CreateUser): Either[Throwable, User] = {
    log.debug(s"Creating user: name=${createUser.name}")
    userRepository.createUser(createUser)
  }

  def getUser(userId: String): Option[User] = {
    log.debug(s"Getting user: id=$userId")
    userRepository.getUser(userId)
  }
}
