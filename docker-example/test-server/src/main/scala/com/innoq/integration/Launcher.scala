package com.innoq.integration

import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

import scala.concurrent.Future

object Launcher extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load

  val server = new Server(config.getInt("server.port"))
  val context = new WebAppContext()

  context.setContextPath("/")
  context.setResourceBase(config.getString("server.resourceBase"))
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[DefaultServlet], "/")

  server.setHandler(context)

  server.start()
  server.join()

  def stopServer(): Future[Unit] = Future(server.stop())
}
