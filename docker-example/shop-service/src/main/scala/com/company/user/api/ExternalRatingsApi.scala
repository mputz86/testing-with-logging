package com.company.user.api

import com.company.user.model.Rating
import org.json4s.jackson.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient

import scala.concurrent.Future


class ExternalRatingsApi(client: WSClient, url: String) {

  import scala.concurrent.ExecutionContext.Implicits.global

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  def getRatings(name: String): Future[Either[Throwable, Seq[Rating]]] =
    client
      .url(s"$url/$name")
      .get
      .map { response =>
        response.status match {
          case 200 =>
            Right(read[Seq[Rating]](response.body))
          case status =>
            Left(new Exception(s"Failed to get ratings: name=$name, status code=$status, response=${response.body}"))
        }
      }.recover {
        case e: Throwable =>
          Left(new Exception(s"Failed to connect to ratings server: name=$name, exception=${e.getMessage}"))
      }
}
