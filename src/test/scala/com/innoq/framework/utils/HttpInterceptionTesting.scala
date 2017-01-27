package com.innoq.framework.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.innoq.framework.TestingBase
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read, write}
import org.scalatest.{Suite, SuiteMixin}
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}
import play.api.libs.ws.ssl.{SSLConfig, SSLLooseConfig}
import play.api.libs.ws.{WSClientConfig, WSResponse}

import scala.concurrent.Await
import scala.concurrent.duration._

object Methods {
  val GET = "GET"
  val PUT = "PUT"
  val POST = "POST"
  val DELETE = "DELETE"
}

object Headers {
  val NO_HEADERS = List()
  val JSON_HEADER = List("Content-Type" -> "application/json")
}

object HttpInterceptionTesting {

  case class Endpoint(path: String, method: String, headers: List[(String, String)])

  case class Response(statusCode: Int, content: Option[String], headers: List[(String, String)])

  case class Interception(endpoint: Endpoint, response: Response)

  case class RequestAndWaitResponse(status: Int, body: String)

  case class ParsedRequestAndWaitResponse[T](status: Int, parsedBody: Option[T], throwable: Option[Throwable])

}


trait ResponseHelpers {

  import HttpInterceptionTesting._

  def emptyResponse = Response(204, None, Headers.NO_HEADERS)

  def jsonResponse(jsonContent: String) = Response(200, Some(jsonContent), Headers.JSON_HEADER)
}

trait HttpInterceptionTesting extends SuiteMixin with ResponseHelpers {
  this: Suite with TestingBase =>

  import HttpInterceptionTesting._

  implicit val timeout = Timeout(20.seconds)
  implicit private val formats = DefaultFormats

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = ActorSystem("test-actor-system")
  implicit val materializer = ActorMaterializer()

  var client: Option[AhcWSClient] = None
  val integrationServerBaseUrl = s"http://${infrastructureConfig.getString("integration.host")}:${infrastructureConfig.getString("integrationServer.port")}"
  val interceptionUri = config.getString("integration.interceptionUri")
  val mocksUri = config.getString("integration.mocksUri")
  val integrationRetryCount = config.getInt("integration.retryCount")
  val integrationRetryDelay = config.getInt("integration.retryDelay")

  val integrationRepositoryHttpCallTimeout = config.getDuration("integration.repositoryHttpCallTimeout").toMillis millis
  val integrationRepositoryHttpCallRetryCount = config.getInt("integration.repositoryHttpCallRetryCount")

  private def closeWsClient() = {
    try {
      client.map(_.close())
    } catch {
      case _: Throwable =>
    }
  }

  abstract override def withFixture(test: NoArgTest) = {
    closeWsClient()

    val configBuilder = new AhcConfigBuilder(AhcWSClientConfig(WSClientConfig(ssl = SSLConfig(loose = SSLLooseConfig(acceptAnyCertificate = true))))).configure()
    configBuilder.setAcceptAnyCertificate(true)
    configBuilder.setSslEngineFactory(null)
    configBuilder.setSslContext(SslContextBuilder.forClient()
      .trustManager(InsecureTrustManagerFactory.INSTANCE).build)
    client = Some(new AhcWSClient(configBuilder.build()))

    try {
      super.withFixture(test)
    } catch {
      case e: Throwable =>
        error(s"Failed within http interception fixture: error=${e.getMessage}", e)
        throw e
    } finally {
      try {
        removeInterceptions()
      } catch {
        case _: Throwable =>
      }
      closeWsClient()
    }
  }

  private def retry[T](title: String, f: () => T, retryCounter: Int, delay: Int): T = {
    try {
      f()
    } catch {
      case t: Throwable =>
        if (retryCounter - 1 > 0) {
          error(s"Failed to ${title}: retry=${retryCounter}, error=${t.getMessage}")
          Thread.sleep(delay)
          retry(title, f, retryCounter - 1, delay)
        } else {
          error(s"Failed to ${title}: retry=${retryCounter}, error=${t.getMessage}", t)
          throw t
        }
    }
  }

  def setInterception(interception: Interception) = {
    retry("set interception", () =>
      client.map { c =>
        info(s"Setting interception ${interception.endpoint.method} ${interception.endpoint.path}")
        val output = write(interception)
        println(output)
        val r = c.url(s"$integrationServerBaseUrl$interceptionUri")
          .withHeaders(("Content-Type" -> "application/json"))
          .put(output)
          .map(response => {
            response.status match {
              case 200 => true
              case e => new RuntimeException(s"Failed to set interception: ${e}")
            }
          })
        Await.result(r, timeout.duration) match {
          case true =>
          case e: Throwable =>
            error(s"Failed to set interception: error=${e.getMessage}", e)
            throw e
        }
      }, integrationRetryCount, integrationRetryDelay)
  }

  def removeInterceptions() = {
    client.map { c =>
      info(s"Removing interceptions")
      val r = c.url(s"$integrationServerBaseUrl$interceptionUri")
        .delete()
        .map(response => {
          response.status match {
            case 200 => true
            case e => new RuntimeException(s"Failed to remove interceptions: ${e}")
          }
        })
      Await.result(r, timeout.duration) match {
        case true =>
        case e: Throwable =>
          error(s"Failed to remove interceptions: error=${e.getMessage}", e)
      }
    }
  }

  def parseResponse[T](response: RequestAndWaitResponse)(implicit manifest: Manifest[T]) = {
    try {
      val parsedBody = read[T](response.body)
      ParsedRequestAndWaitResponse(response.status, Some(parsedBody), None)
    } catch {
      case t: Throwable =>
        ParsedRequestAndWaitResponse(response.status, None, Some(t))
    }
  }

  def requestAndWait(url: String, method: String, content: String = "", headers: Map[String, String] = Map("Content-Type" -> "application/json"), timeoutOpt: Option[FiniteDuration] = Some(30 seconds), retryCountOpt: Option[Int] = Some(16), retryDelay: Option[Int] = Some(15000)) = {
    val timeout: FiniteDuration = timeoutOpt.getOrElse(integrationRepositoryHttpCallTimeout)
    val retryCount = retryCountOpt.getOrElse(integrationRepositoryHttpCallRetryCount)

    info(s"Requesting and waiting: url=${url}, method=${method}, content=${content}")
    retry("request and wait", { () =>
      client.map { c =>
        val request = c.url(url)
          .withHeaders(headers.toSeq: _*)
          .withBody(content)
          .withMethod(method.toUpperCase).execute()
          .map(response => {
            response.status match {
              case 502 =>
                new RuntimeException("Received 502 Bad Gateway, retrying")
              case 504 =>
                new RuntimeException("Received 504 Gateway Timeout, retrying")
              case _ =>
                info(s"<< ${method.toUpperCase} ${url}: status=${response.status}, body=${response.body}")
                response

            }
          })
        Await.result(request, timeout) match {
          case e: Throwable =>
            error(s"Failed to request and wait ${c.config.isAcceptAnyCertificate}: error=${e.getMessage}, url=${url}, method=${method}", e)
            throw e
          case response: WSResponse => RequestAndWaitResponse(response.status, response.body)
        }
      }.get
    }, retryCount, retryDelay.getOrElse(0))
  }
}
