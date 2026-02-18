package com.zollector.marketplace.services

import com.zollector.marketplace.config.JWTConfig
import com.zollector.marketplace.domain.data.User
import zio.*
import zio.test.*

object JWTServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service <- ZIO.service[JWTService]
          clock   <- Clock.javaClock
          now     <- ZIO.attempt(clock.instant)
          user = User(
            1L,
            "aNickname",
            "admin@zollector.com",
            "bob",
            "lazar",
            "1000:4A6F579C455FBA4C0ED77A1517EF9546D5E1A3B8EF9E3033:C4947E73B50A06A5CF3B2EF3036718BFB8E763BDEECF0B1B", // See UserServiceDemo in UserService
            now
          )
          userToken <- service.createToken(user)
          userId    <- service.verityToken(userToken.token)
        } yield assertTrue(userId.id == 1L && userId.email == user.email)
      }
    ).provide(JWTServiceLive.layer, ZLayer.succeed(JWTConfig("secret", 3600)))
}
