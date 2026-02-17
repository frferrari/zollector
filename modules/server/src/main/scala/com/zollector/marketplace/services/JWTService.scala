package com.zollector.marketplace.services

import zio.*

import java.time.Instant
import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.zollector.marketplace.domain.data.*

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verityToken(token: String): Task[UserID]
}

class JWTServiceLive(clock: java.time.Clock) extends JWTService {

  private val SECRET    = "secret"       // TODO parse from config
  private val ISSUER    = "zollector.com"
  private val TTL       = 30 * 24 * 3600 // TODO parse from config
  private val algorithm = Algorithm.HMAC512(SECRET)
  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  private val CLAIM_EMAIL = "email"

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant())
      expiration <- ZIO.succeed(now.plusSeconds(TTL))
      token <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString)
          .withClaim(CLAIM_EMAIL, user.email)
          .sign(algorithm)
      )
    } yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verityToken(token: String): Task[UserID] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserID(decoded.getSubject.toLong, decoded.getClaim(CLAIM_EMAIL).asString())
      )
    } yield userId
}

object JWTServiceLive {
  val layer = ZLayer {
    Clock.javaClock.map(clock => new JWTServiceLive(clock))
  }
}

object JWTServiceDemo extends ZIOAppDefault {
  val program = for {
    service <- ZIO.service[JWTService]
    userToken <- service.createToken(
      User(
        1L,
        "nickname",
        "admin@zollector.com",
        "firstname",
        "lastname",
        "hashedpassword",
        java.time.Instant.now()
      )
    )
    _      <- Console.printLine(userToken)
    userId <- service.verityToken(userToken.token)
    _      <- Console.printLine(userId.toString)
  } yield ()

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program.provide(JWTServiceLive.layer)
}
