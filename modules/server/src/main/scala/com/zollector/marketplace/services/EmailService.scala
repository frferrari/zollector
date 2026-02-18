package com.zollector.marketplace.services

import zio.*
import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

import com.zollector.marketplace.config.*

trait EmailService {
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "Zollector: Password recovery"
    val content =
      s"""
         |<div style="border: 1px solid black; padding: 20px; font-family: sans-serif; line-height: 2; font-size: 20px">
         |  <h1>Zollector: Password Recovery</h1>
         |  <p>Your password recovery token is <strong>$token</strong></p>
         |</div>""".stripMargin

    sendEmail(to, subject, content)
  }
}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {

  private val host: String     = config.host
  private val port: Int        = config.port
  private val user: String     = config.user
  private val password: String = config.password

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    for {
      prop    <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)("noreply@zollection.com", to, subject, content)
      _       <- ZIO.attempt(Transport.send(message))
    } yield ()
  }

  private val propsResource: Task[Properties] =
    ZIO.attempt {
      val prop = new Properties
      prop.put("mail.smtp.auth", true)
      prop.put("mail.smtp.starttls.enable", true)
      prop.put("mail.smtp.host", host)
      prop.put("mail.smtp.port", port)
      prop.put("mail.smtp.ssl.trust", host)

      prop
    }

  private def createSession(prop: Properties): Task[Session] =
    ZIO.attempt {
      Session.getInstance(
        prop,
        new Authenticator {
          override protected def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(user, password)
        }
      )
    }

  private def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): Task[MimeMessage] =
    ZIO.attempt {
      val message = new MimeMessage(session)
      message.setFrom(from)
      message.setRecipients(Message.RecipientType.TO, to)
      message.setSubject(subject)
      message.setContent(content, "text/html; charset=utf-8")

      message
    }
}

object EmailServiceLive {
  val layer = ZLayer {
    ZIO.service[EmailServiceConfig].map(config => new EmailServiceLive(config))
  }

  val configuredLayer = Configs.makeLayer[EmailServiceConfig](Configs.CONFIG_EMAIL) >>> layer

}

object EmailServiceDemo extends ZIOAppDefault {
  val program = for {
    emailService <- ZIO.service[EmailService]
    // _            <- emailService.sendEmail("boblazar@zollector.com", "Hi from a UAP", "This email service rocks")
    _ <- emailService.sendPasswordRecoveryEmail("boblazar@zollector.com", "ABCD1234")
    _ <- Console.printLine("Email done.")
  } yield ()

  override def run = program.provide(EmailServiceLive.configuredLayer)
}
