import javax.servlet.ServletContext

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.company.Launcher
import com.company.controller.ShopController
import com.company.service.ShopService
import com.company.user.api.{ExternalRatingsApi, UserApi}
import com.typesafe.config.ConfigFactory
import org.scalatra._
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClientConfig
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}

import scala.language.postfixOps


class ScalatraBootstrap extends LifeCycle {

  val config = ConfigFactory.load
  val log = LoggerFactory.getLogger(classOf[ScalatraBootstrap])

  implicit val system = ActorSystem("shop-service", config)
  implicit val materializer = ActorMaterializer()

  val clientConfig = new AhcConfigBuilder(AhcWSClientConfig(WSClientConfig())).build()

  val client = new AhcWSClient(clientConfig)

  override def init(context: ServletContext) {

    val userUrl = config.getString("apis.user.url")
    val externalRatingsUrl = config.getString("apis.external.ratings.url")

    val userApi = new UserApi(client, userUrl)
    val externalRatingsApi = new ExternalRatingsApi(client, externalRatingsUrl)
    val shopService = new ShopService(userApi, externalRatingsApi)
    val shopController = new ShopController(shopService)

    try {
      context.mount(shopController, "/shop/*")
    } catch {
      case e: Throwable =>
        Launcher.stopServer()
        throw e
    }
  }

  override def destroy(context: ServletContext): Unit = {
  }
}
