package com.innoq.framework

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

class LogMessageTestingActor(verbose: Boolean = false) extends Actor with TestLogger {

  var firstMessage = true
  var store = mutable.Buffer[LogMessage]()
  var waiters = mutable.Map[ActorRef, WaitFor]()

  if (verbose) info("Verbose logging")

  override def postStop = {
    if (firstMessage) {
      warn(s"No log message was received")
    }
  }

  def receive = {
    case logMessage: LogMessage =>
      if (verbose) LogMessage.printLogMessage(logMessage)
      firstMessage = false

      waiters.find { case (_, waitFor) =>
        matchesWaitFor(logMessage, waitFor)
      } map { case waiter@(sender, _) =>
        waiters -= sender
        sender ! logMessage
        true
      } match {
        case Some(_) =>
        case None =>
          store += logMessage
      }

    case waitFor: WaitFor =>
      store.find {
        matchesWaitFor(_, waitFor)
      } match {
        case Some(logMessage) =>
          store = store -= logMessage
          sender ! logMessage
        case None =>
          waiters += ((sender, waitFor))
      }
    case Clear =>
      store.clear()
      waiters.clear()

    case _ => ;
  }

  private def matchesWaitFor(logMessage: LogMessage, waitFor: WaitFor): Boolean = {
    waitFor.level.equals(logMessage.level) &&
      waitFor.appName.matcher(logMessage.appName.getOrElse("")).matches() &&
      waitFor.message.matcher(logMessage.message).matches()
  }
}
