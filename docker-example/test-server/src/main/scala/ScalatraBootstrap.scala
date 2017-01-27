import javax.servlet.ServletContext

import akka.actor.{ActorSystem, Props}
import com.innoq.integration.{InterceptionController, InterceptionHandler, Launcher, MockController}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val actorSystem = ActorSystem("test-server")

    val interceptionHandler = actorSystem.actorOf(Props[InterceptionHandler])

    try {
      context.mount(new InterceptionController(actorSystem, interceptionHandler), "/interceptions/*")
      context.mount(new MockController(actorSystem, interceptionHandler), "/mocks/*")
    } catch {
      case e: Throwable =>
        Launcher.stopServer()
        throw e
    }
  }
}
