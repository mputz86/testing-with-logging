package com.company.user.service

import com.company.model.User
import com.company.repository.UserRepository
import org.slf4j.LoggerFactory

class AccountService(userRepository: UserRepository) {

  val log = LoggerFactory.getLogger(classOf[AccountService])

  def creditUser(userId: String, amount: Int): Either[Throwable, User] = {
    log.debug(s"Crediting user: user id=$userId, amount=$amount")
    userRepository.creditUser(userId, amount)
  }

  def debitUser(userId: String, amount: Int): Either[Throwable, User] = {
    log.debug(s"Debiting user: user id=$userId, amount=$amount")
    userRepository.debitUser(userId, amount)
  }
}
