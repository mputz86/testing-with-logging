import javax.servlet.ServletContext

import com.company.Launcher
import com.company.controller.UserController
import com.company.repository.UserRepository
import com.company.service.UserService
import com.company.user.service.AccountService
import com.typesafe.config.ConfigFactory
import org.scalatra._
import org.slf4j.LoggerFactory

import scala.language.postfixOps


class ScalatraBootstrap extends LifeCycle {

  val config = ConfigFactory.load

  override def init(context: ServletContext) {

    val userRepository = new UserRepository()
    val userService = new UserService(userRepository)
    val paymentService = new AccountService(userRepository)
    val userController = new UserController(userService, paymentService)

    try {
      context.mount(userController, "/users/*")
    } catch {
      case e: Throwable =>
        Launcher.stopServer()
        throw e
    }
  }

  override def destroy(context: ServletContext): Unit = {
  }
}
