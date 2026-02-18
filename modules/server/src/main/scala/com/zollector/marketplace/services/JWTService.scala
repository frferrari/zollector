package com.zollector.marketplace.services

import zio.*
import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

import com.zollector.marketplace.config.{Configs, JWTConfig}
import com.zollector.marketplace.domain.data.*

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verityToken(token: String): Task[UserID]
}

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {
  private val ISSUER    = "zollector.com"
  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
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
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
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
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer = Configs.makeLayer[JWTConfig](Configs.CONFIG_JWT) >>> layer
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
    program.provide(
      JWTServiceLive.layer,
      Configs.makeLayer[JWTConfig](Configs.CONFIG_JWT)
    )
}
