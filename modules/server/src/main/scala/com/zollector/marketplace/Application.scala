package com.zollector.marketplace

import com.zollector.marketplace.config.{Configs, JWTConfig}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*
import com.zollector.marketplace.http.HttpApi
import com.zollector.marketplace.services.*
import com.zollector.marketplace.repositories.*

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(endpoints)
    )
    _ <- Console.printLine("... Starting ...")
  } yield ()

  override def run = serverProgram.provide(
    Server.default,
    // services
    CollectionServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,
    // repos
    CollectionRepositoryLive.layer,
    UserRepositoryLive.layer,
    RecoveryTokensRepositoryLive.configuredLayer,
    // other requirements
    Repository.dataLayer
  )
}
